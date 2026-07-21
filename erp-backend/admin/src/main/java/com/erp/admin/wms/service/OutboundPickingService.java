package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.OutboundPickingMapper;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickAllocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsPalletMapper;
import com.erp.admin.wms.model.dto.PickDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.qo.OutboundPickingQO;
import com.erp.admin.wms.model.vo.OutboundOrderItemVO;
import com.erp.admin.wms.model.vo.OutboundOrderVO;
import com.erp.admin.wms.model.vo.PickAllocationVO;
import com.erp.admin.wms.model.vo.PickListVO;
import com.erp.admin.wms.model.vo.PickerVO;
import com.erp.admin.wms.model.vo.StockShortageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 海外仓平台出库作业·下架(拣货)服务（业务需求 1.3.1）。
 *
 * <p>基于既有销售出库单 {@code wms_sales_outbound_order}（不改货主侧下单逻辑）。平台视角：
 * 货主确认后 DB=CONFIRMED 即"待下架 PENDING"。下架执行 FIFO 分配（良品/单仓/inbound_date→pick_order→id 升序，
 * SELECT FOR UPDATE，reserved_qty += take）；缺货整单挂起 BACKORDER 不部分下架；转 PICKING 后不可取消。</p>
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundPickingService {

    /**
     * 下架页默认作用域（DB 状态）：待下架 + 拣货中 + 缺货挂起 + 拣货完成(打包/签出/完成)。
     * 打包及之后的单保留在下架页，统一以"拣货完成"展示（见 {@link #toPickingViewStatus}）。
     */
    private static final List<String> DEFAULT_SCOPE = Arrays.asList(
            OutboundOrderStatus.CONFIRMED.name(),
            OutboundOrderStatus.PICKING.name(),
            OutboundOrderStatus.BACKORDER.name(),
            OutboundOrderStatus.PACKED.name(),
            OutboundOrderStatus.SHIPPED.name(),
            OutboundOrderStatus.COMPLETED.name());

    private final OutboundPickingMapper outboundPickingMapper;

    private final SalesOutboundMapper salesOutboundMapper;

    private final SalesOutboundItemMapper salesOutboundItemMapper;

    private final WmsPhysicalInventoryMapper physicalInventoryMapper;

    private final WmsOutboundPickAllocationMapper pickAllocationMapper;

    private final WmsInventoryAggregator inventoryAggregator;

    private final TenantIdentityService tenantIdentityService;

    private final WmsPalletMapper palletMapper;

    private final WmsPalletService palletService;

    // ==================== 查询 ====================

    public PageResult<OutboundOrderVO> page(PageParam pageParam, OutboundPickingQO qo) {
        assertPlatform();
        // 下架页视图状态 → DB 状态集合（PICKED 覆盖打包/签出/完成）
        if (qo.getStatus() != null && !qo.getStatus().isEmpty()) {
            qo.setDbStatuses(pickingDbStatuses(qo.getStatus()));
        } else {
            qo.setDbStatuses(DEFAULT_SCOPE);
        }
        IPage<OutboundOrderVO> page = PageUtil.prodPage(pageParam);
        outboundPickingMapper.pageOrders(page, qo);
        List<OutboundOrderVO> records = page.getRecords();
        records.forEach(r -> r.setStatus(toPickingViewStatus(r.getStatus())));
        return new PageResult<>(records, page.getTotal());
    }

    public OutboundOrderVO getDetail(Long id) {
        assertPlatform();
        OutboundOrderVO vo = outboundPickingMapper.selectOrderById(id);
        Assert.notNull(vo, "出库单不存在");
        vo.setItems(buildItemsWithAvailability(vo));
        vo.setStatus(toPickingViewStatus(vo.getStatus()));
        return vo;
    }

    /** FIFO 分配预览（只算不锁） */
    public List<PickAllocationVO> preview(Long id) {
        assertPlatform();
        OutboundOrderVO vo = outboundPickingMapper.selectOrderById(id);
        Assert.notNull(vo, "出库单不存在");
        List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(id);
        Map<String, Integer> required = requiredBySku(items);

        List<PickAllocationVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : required.entrySet()) {
            List<WmsPhysicalInventory> batches = physicalInventoryMapper
                    .selectFifoAllocatable(0L, vo.getErpTenantId(), vo.getWarehouseId(), e.getKey());
            preferLoosePallets(batches);
            AllocPlan plan = planAllocation(batches, e.getValue());
            for (Take t : plan.takes) {
                result.add(toAllocationVO(e.getKey(), t.batch, t.take));
            }
        }
        return result;
    }

    public PickListVO getPickList(Long id) {
        assertPlatform();
        OutboundOrderVO vo = outboundPickingMapper.selectOrderById(id);
        Assert.notNull(vo, "出库单不存在");
        List<WmsOutboundPickAllocation> allocs = pickAllocationMapper.selectByOutboundOrderId(id);
        PickListVO pl = new PickListVO();
        pl.setOutboundNo(vo.getOutboundNo());
        pl.setOwnerName(vo.getOwnerName());
        pl.setWarehouseName(vo.getWarehouseName());
        pl.setPickMode(vo.getPickMode());
        pl.setPickerName(vo.getPickerName());
        List<PickAllocationVO> lines = new ArrayList<>();
        for (WmsOutboundPickAllocation a : allocs) {
            PickAllocationVO av = new PickAllocationVO();
            av.setSkuCode(a.getSkuCode());
            av.setLocationCode(a.getLocationCode());
            av.setInboundDate(a.getInboundDate() == null ? null : a.getInboundDate().toString());
            av.setBatchNo(batchNo(a.getInboundDate() == null ? null : a.getInboundDate().toString(), a.getPickOrder()));
            av.setTakeQty(a.getTakeQty());
            lines.add(av);
        }
        pl.setAllocations(lines);
        return pl;
    }

    public List<PickerVO> listPickers() {
        assertPlatform();
        return outboundPickingMapper.selectPickers();
    }

    // ==================== 下架（FIFO 锁定，防超卖 C7） ====================

    /**
     * 确认下架：两阶段（先锁批次算全量分配，全部够才应用）。
     * 任一 SKU 不足 → 整单挂起 BACKORDER（不部分下架），返回失败提示；成功 → 转 PICKING。
     */
    @Transactional(rollbackFor = Exception.class)
    public PickResult pick(PickDTO dto) {
        assertPlatform();
        SalesOutboundOrder order = salesOutboundMapper.selectById(dto.getOutboundOrderId());
        Assert.notNull(order, "出库单不存在");
        String st = order.getOrderStatus();
        // 幂等 + 前置校验：仅 待下架(CONFIRMED) 或 缺货挂起(BACKORDER, 允许补货后重试) 可下架
        if (OutboundOrderStatus.PICKING.name().equals(st) || OutboundOrderStatus.PACKED.name().equals(st)
                || OutboundOrderStatus.SHIPPED.name().equals(st) || OutboundOrderStatus.COMPLETED.name().equals(st)) {
            throw new BusinessException(400, "该出库单已下架，请勿重复操作");
        }
        if (!OutboundOrderStatus.CONFIRMED.name().equals(st) && !OutboundOrderStatus.BACKORDER.name().equals(st)) {
            throw new BusinessException(400, "仅待下架的出库单可以下架");
        }

        // 状态 CAS 抢占：CONFIRMED/BACKORDER→PICKING 原子推进，并发/双击下架只有一个赢家（消灭 TOCTOU）。
        // 落败者在此即被挡下，绝不进入下方兜底补预留分支，杜绝同单并发双分配/双扣减。
        int claimed = salesOutboundMapper.casOrderStatus(order.getId(), st, OutboundOrderStatus.PICKING.name());
        if (claimed != 1) {
            throw new BusinessException(400, "该出库单已下架或正在处理，请勿重复操作");
        }

        // 批次预留已在“确认(confirm)”时完成（方案A）。下架仅推进状态 + 指派拣货员。
        // 兜底：历史/异常单若确认时未建预留分配，则在此补做，保持向后兼容。
        List<WmsOutboundPickAllocation> allocations = pickAllocationMapper.selectByOutboundOrderId(order.getId());
        if (allocations.isEmpty()) {
            List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(order.getId());
            Assert.notEmpty(items, "出库单无明细");
            List<StockShortageVO> shortages = reserveForOrder(order, items);
            if (!shortages.isEmpty()) {
                order.setOrderStatus(OutboundOrderStatus.BACKORDER.name());
                salesOutboundMapper.updateById(order);
                log.info("下架补预留缺货整单挂起, outboundId={}, shortageSku={}", order.getId(), shortages.get(0).getSkuCode());
                return PickResult.fail("SKU[" + shortages.get(0).getSkuCode() + "] 可用良品不足，整单已挂起(缺货)");
            }
        }

        order.setOrderStatus(OutboundOrderStatus.PICKING.name());
        order.setPickMode(dto.getPickMode());
        order.setPickerId(dto.getPickerId());
        order.setPickerName(outboundPickingMapper.selectPickerName(dto.getPickerId()));
        salesOutboundMapper.updateById(order);
        log.info("下架完成, outboundId={}, picker={}", order.getId(), dto.getPickerId());
        return PickResult.ok();
    }

    // ==================== 批次预留 / 释放（供确认/取消/下架复用） ====================

    /**
     * 为出库单预留批次（确认出库时调用；下架无分配时兜底）。
     * <p>FIFO 锁批次 → 计算全量分配；任一 SKU 缺口&gt;0 则不应用任何预留、返回全部缺货明细；
     * 全部够则逐批次 {@code reserved_qty += take} + 建拣货分配 + 同事务刷新快照。
     * 事务由调用方持有；返回非空即缺货，调用方据此回滚/挂起。</p>
     */
    public List<StockShortageVO> reserveForOrder(SalesOutboundOrder order, List<SalesOutboundOrderItem> items) {
        Map<String, Integer> required = requiredBySku(items);
        List<Take> planned = new ArrayList<>();
        List<StockShortageVO> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> e : required.entrySet()) {
            List<WmsPhysicalInventory> batches = physicalInventoryMapper
                    .selectFifoAllocatableForUpdate(0L, order.getErpTenantId(), order.getWarehouseId(), e.getKey());
            preferLoosePallets(batches);
            AllocPlan plan = planAllocation(batches, e.getValue());
            if (plan.shortage > 0) {
                StockShortageVO s = new StockShortageVO();
                s.setSkuCode(e.getKey());
                s.setSkuName(e.getKey());
                s.setRequiredQty(e.getValue());
                s.setAvailableQty(e.getValue() - plan.shortage);
                s.setShortage(plan.shortage);
                shortages.add(s);
            } else {
                planned.addAll(plan.takes);
            }
        }
        // 有任一缺货：不应用任何预留，返回全部缺货明细（前端 StockShortageModal 展示）
        if (!shortages.isEmpty()) {
            return shortages;
        }
        java.util.Set<String> touchedSku = new java.util.LinkedHashSet<>();
        java.util.Set<Long> touchedPallets = new java.util.LinkedHashSet<>();
        for (Take t : planned) {
            WmsPhysicalInventory b = t.batch;
            b.setReservedQty((b.getReservedQty() == null ? 0 : b.getReservedQty()) + t.take);
            physicalInventoryMapper.updateById(b);

            WmsOutboundPickAllocation alloc = new WmsOutboundPickAllocation();
            alloc.setOutboundOrderId(order.getId());
            alloc.setPhysicalInventoryId(b.getId());
            alloc.setSkuCode(b.getSkuCode());
            alloc.setLocationCode(b.getLocationCode());
            alloc.setInboundDate(b.getInboundDate());
            alloc.setPickOrder(b.getPickOrder());
            alloc.setTakeQty(t.take);
            pickAllocationMapper.insert(alloc);
            touchedSku.add(b.getSkuCode());
            if (b.getPalletId() != null) {
                touchedPallets.add(b.getPalletId());
            }
        }
        for (String sku : touchedSku) {
            inventoryAggregator.refreshSnapshot(0L, order.getErpTenantId(), order.getWarehouseId(), sku);
        }
        for (Long palletId : touchedPallets) {
            palletService.refreshAfterInventoryChange(palletId);
        }
        return java.util.Collections.emptyList();
    }

    /** Partial and mixed pallets are consumed before homogeneous full pallets, then FIFO is preserved. */
    private void preferLoosePallets(List<WmsPhysicalInventory> batches) {
        java.util.Set<Long> palletIds = batches.stream().map(WmsPhysicalInventory::getPalletId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, WmsPallet> pallets = palletIds.isEmpty() ? java.util.Collections.emptyMap()
                : palletMapper.selectBatchIds(palletIds).stream()
                .collect(java.util.stream.Collectors.toMap(WmsPallet::getId, value -> value));
        java.util.Comparator<WmsPhysicalInventory> fifo = java.util.Comparator
                .comparing(WmsPhysicalInventory::getInboundDate, java.util.Comparator.nullsLast(java.time.LocalDate::compareTo))
                .thenComparing(WmsPhysicalInventory::getPickOrder, java.util.Comparator.nullsLast(Integer::compareTo))
                .thenComparing(WmsPhysicalInventory::getId);
        batches.sort(java.util.Comparator.comparingInt((WmsPhysicalInventory batch) -> {
            WmsPallet pallet = pallets.get(batch.getPalletId());
            return pallet != null && Integer.valueOf(1).equals(pallet.getWholePalletEligible()) ? 1 : 0;
        }).thenComparing(fifo));
    }

    /**
     * 释放出库单已预留的批次（取消出库时调用，仅下架前）。
     * <p>按拣货分配逐批次 {@code reserved_qty -= take} + 删分配 + 同事务刷新快照。
     * 事务由调用方持有。批次 @Version 冲突则抛出，回滚整个取消。</p>
     */
    public void releaseForOrder(SalesOutboundOrder order) {
        List<WmsOutboundPickAllocation> allocations = pickAllocationMapper.selectByOutboundOrderId(order.getId());
        java.util.Set<String> touchedSku = new java.util.LinkedHashSet<>();
        for (WmsOutboundPickAllocation a : allocations) {
            WmsPhysicalInventory b = physicalInventoryMapper.selectById(a.getPhysicalInventoryId());
            if (b != null) {
                int take = a.getTakeQty() == null ? 0 : a.getTakeQty();
                int newReserved = (b.getReservedQty() == null ? 0 : b.getReservedQty()) - take;
                b.setReservedQty(Math.max(newReserved, 0));
                int updated = physicalInventoryMapper.updateById(b);
                if (updated == 0) {
                    throw new BusinessException(409, "库存并发变更，请重试取消");
                }
                touchedSku.add(b.getSkuCode());
            }
            pickAllocationMapper.deleteById(a.getId());
        }
        for (String sku : touchedSku) {
            inventoryAggregator.refreshSnapshot(0L, order.getErpTenantId(), order.getWarehouseId(), sku);
        }
    }

    // ==================== 组装 / 纯逻辑 ====================

    private List<OutboundOrderItemVO> buildItemsWithAvailability(OutboundOrderVO order) {
        List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(order.getId());
        Map<String, Integer> required = requiredBySku(items);
        List<OutboundOrderItemVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : required.entrySet()) {
            List<WmsPhysicalInventory> batches = physicalInventoryMapper
                    .selectFifoAllocatable(0L, order.getErpTenantId(), order.getWarehouseId(), e.getKey());
            int available = availableQty(batches);
            OutboundOrderItemVO vo = new OutboundOrderItemVO();
            vo.setSkuCode(e.getKey());
            vo.setRequiredQty(e.getValue());
            vo.setAvailableQty(available);
            vo.setShortage(available < e.getValue());
            result.add(vo);
        }
        return result;
    }

    private Map<String, Integer> requiredBySku(List<SalesOutboundOrderItem> items) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (SalesOutboundOrderItem i : items) {
            m.merge(i.getSkuCode(), i.getQuantity() == null ? 0 : i.getQuantity(), Integer::sum);
        }
        return m;
    }

    private PickAllocationVO toAllocationVO(String sku, WmsPhysicalInventory b, int take) {
        PickAllocationVO av = new PickAllocationVO();
        av.setSkuCode(sku);
        av.setLocationCode(b.getLocationCode());
        av.setInboundDate(b.getInboundDate() == null ? null : b.getInboundDate().toString());
        av.setBatchNo(batchNo(b.getInboundDate() == null ? null : b.getInboundDate().toString(), b.getPickOrder()));
        av.setTakeQty(take);
        return av;
    }

    private static String batchNo(String inboundDate, Integer pickOrder) {
        return (inboundDate == null ? "" : inboundDate) + "-" + (pickOrder == null ? 0 : pickOrder);
    }

    /** 可用良品数 = Σ(quantity - reserved_qty) */
    public static int availableQty(List<WmsPhysicalInventory> batches) {
        int sum = 0;
        for (WmsPhysicalInventory b : batches) {
            int avail = (b.getQuantity() == null ? 0 : b.getQuantity())
                    - (b.getReservedQty() == null ? 0 : b.getReservedQty());
            if (avail > 0) {
                sum += avail;
            }
        }
        return sum;
    }

    /** FIFO 分配（批次须已按 inbound_date→pick_order→id 升序）：取到满足或耗尽，返回取货计划与缺口。 */
    public static AllocPlan planAllocation(List<WmsPhysicalInventory> batches, int required) {
        List<Take> takes = new ArrayList<>();
        int remaining = required;
        for (WmsPhysicalInventory b : batches) {
            if (remaining <= 0) {
                break;
            }
            int avail = (b.getQuantity() == null ? 0 : b.getQuantity())
                    - (b.getReservedQty() == null ? 0 : b.getReservedQty());
            if (avail <= 0) {
                continue;
            }
            int take = Math.min(avail, remaining);
            takes.add(new Take(b, take));
            remaining -= take;
        }
        return new AllocPlan(takes, Math.max(remaining, 0));
    }

    private void assertPlatform() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可执行出库作业");
        }
    }

    /** DB 状态 → 平台视角状态（CONFIRMED=待下架 PENDING） */
    public static String toViewStatus(String dbStatus) {
        if (OutboundOrderStatus.CONFIRMED.name().equals(dbStatus)) {
            return "PENDING";
        }
        return dbStatus;
    }

    /** 平台视角状态 → DB 状态 */
    public static String toDbStatus(String viewStatus) {
        if ("PENDING".equals(viewStatus)) {
            return OutboundOrderStatus.CONFIRMED.name();
        }
        return viewStatus;
    }

    /**
     * 下架页专用：DB 状态 → 下架页视图状态。
     * <p>CONFIRMED→待下架(PENDING)；打包/签出/完成 统一→拣货完成(PICKED)；PICKING/BACKORDER 透传。
     * 与共用的 {@link #toViewStatus} 分开，避免影响打包签出页对 已打包/已发货/完成 的区分展示。
     */
    public static String toPickingViewStatus(String dbStatus) {
        if (OutboundOrderStatus.CONFIRMED.name().equals(dbStatus)) {
            return "PENDING";
        }
        if (OutboundOrderStatus.PACKED.name().equals(dbStatus)
                || OutboundOrderStatus.SHIPPED.name().equals(dbStatus)
                || OutboundOrderStatus.COMPLETED.name().equals(dbStatus)) {
            return "PICKED";
        }
        return dbStatus;
    }

    /** 下架页专用：视图状态 → DB 状态集合（PICKED 覆盖 打包/签出/完成）。 */
    public static List<String> pickingDbStatuses(String viewStatus) {
        if ("PENDING".equals(viewStatus)) {
            return java.util.Collections.singletonList(OutboundOrderStatus.CONFIRMED.name());
        }
        if ("PICKED".equals(viewStatus)) {
            return Arrays.asList(OutboundOrderStatus.PACKED.name(),
                    OutboundOrderStatus.SHIPPED.name(), OutboundOrderStatus.COMPLETED.name());
        }
        return java.util.Collections.singletonList(viewStatus);
    }

    // ---- 内部结构 ----

    public static class Take {
        public final WmsPhysicalInventory batch;
        public final int take;

        public Take(WmsPhysicalInventory batch, int take) {
            this.batch = batch;
            this.take = take;
        }
    }

    public static class AllocPlan {
        public final List<Take> takes;
        public final int shortage;

        public AllocPlan(List<Take> takes, int shortage) {
            this.takes = takes;
            this.shortage = shortage;
        }
    }

    /** 下架结果（供 controller 转 ApiResult；缺货挂起时 ok=false 且状态已落 BACKORDER） */
    public static class PickResult {
        private final boolean ok;
        private final String message;

        private PickResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static PickResult ok() {
            return new PickResult(true, "下架成功");
        }

        public static PickResult fail(String message) {
            return new PickResult(false, message);
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }

}
