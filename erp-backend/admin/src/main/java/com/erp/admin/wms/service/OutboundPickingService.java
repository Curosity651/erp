package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.product.service.SkuBarcodeService;
import com.erp.admin.wms.mapper.OutboundPickingMapper;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickAllocationMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickTaskLineMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickTaskMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickTaskOrderMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsOutboundScanEventMapper;
import com.erp.admin.wms.mapper.WmsPalletMapper;
import com.erp.admin.wms.model.dto.PickDTO;
import com.erp.admin.wms.model.dto.BatchPickDTO;
import com.erp.admin.wms.model.dto.BatchPickPreviewDTO;
import com.erp.admin.wms.model.dto.PickExceptionDTO;
import com.erp.admin.wms.model.dto.PickLineScanDTO;
import com.erp.admin.wms.model.dto.ResolvePickExceptionDTO;
import com.erp.admin.wms.model.dto.PackageScanDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import com.erp.admin.wms.model.entity.WmsOutboundPickTask;
import com.erp.admin.wms.model.entity.WmsOutboundPickTaskLine;
import com.erp.admin.wms.model.entity.WmsOutboundPickTaskOrder;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.entity.WmsOutboundScanEvent;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.qo.OutboundPickingQO;
import com.erp.admin.wms.model.vo.OutboundOrderItemVO;
import com.erp.admin.wms.model.vo.OutboundOrderVO;
import com.erp.admin.wms.model.vo.PickAllocationVO;
import com.erp.admin.wms.model.vo.PickListVO;
import com.erp.admin.wms.model.vo.PickerVO;
import com.erp.admin.wms.model.vo.PickTaskOutboundVO;
import com.erp.admin.wms.model.vo.StockShortageVO;
import com.erp.admin.wms.model.vo.BatchPickPreviewVO;
import com.erp.admin.wms.model.vo.BatchPickResultVO;
import com.erp.admin.wms.model.vo.PickTaskPreviewVO;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

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
            OutboundOrderStatus.PICKED.name(),
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

    private final WmsOutboundPickTaskMapper pickTaskMapper;

    private final WmsOutboundPickTaskOrderMapper pickTaskOrderMapper;

    private final WmsOutboundPickTaskLineMapper pickTaskLineMapper;

	private final SalesOutboundPackageService outboundPackageService;

    private final SkuBarcodeService skuBarcodeService;

    private final WmsOutboundScanEventMapper scanEventMapper;

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
        records.forEach(r -> {
            attachTask(r);
            r.setStatus(toPickingViewStatus(r.getStatus()));
        });
        return new PageResult<>(records, page.getTotal());
    }

    public OutboundOrderVO getDetail(Long id) {
        assertPlatform();
        OutboundOrderVO vo = outboundPickingMapper.selectOrderById(id);
        Assert.notNull(vo, "出库单不存在");
        vo.setItems(buildItemsWithAvailability(vo));
        attachTask(vo);
        vo.setStatus(toPickingViewStatus(vo.getStatus()));
        return vo;
    }

    /** FIFO 分配预览（只算不锁） */
    public List<PickAllocationVO> preview(Long id) {
        assertPlatform();
        OutboundOrderVO vo = outboundPickingMapper.selectOrderById(id);
        Assert.notNull(vo, "出库单不存在");
        List<WmsOutboundPickAllocation> reserved = pickAllocationMapper.selectByOutboundOrderId(id);
        if (!reserved.isEmpty()) {
            return reserved.stream().map(this::toAllocationVO).collect(Collectors.toList());
        }
        List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(id);
        Map<String, Integer> required = requiredBySku(items);

        List<PickAllocationVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : required.entrySet()) {
            List<WmsPhysicalInventory> batches = physicalInventoryMapper
					.selectFifoAllocatable(vo.getErpTenantId(), vo.getWarehouseId(), e.getKey());
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
        WmsOutboundPickTaskOrder taskOrder = pickTaskOrderMapper.selectLatestByOrderId(id);
        List<WmsOutboundPickAllocation> allocs = pickAllocationMapper.selectByOutboundOrderId(id);
        PickListVO pl = new PickListVO();
        pl.setOutboundNo(vo.getOutboundNo());
        pl.setSourceType(vo.getSourceType());
        pl.setOwnerName(vo.getOwnerName());
        pl.setWarehouseName(vo.getWarehouseName());
        pl.setPickMode(vo.getPickMode());
        pl.setPickerName(vo.getPickerName());
        List<PickAllocationVO> lines = new ArrayList<>();
        List<PickTaskOutboundVO> taskOutbounds = new ArrayList<>();
        if (taskOrder != null) {
            WmsOutboundPickTask task = pickTaskMapper.selectById(taskOrder.getTaskId());
            if (task != null && !"CANCELLED".equals(task.getTaskStatus())) {
                pl.setTaskId(task.getId());
                pl.setTaskNo(task.getTaskNo());
                pl.setTaskType(task.getTaskType());
                pl.setTaskStatus(task.getTaskStatus());
                pl.setOutboundOrderCount(task.getOrderCount());
                pl.setSalesOrderCount(task.getSalesOrderCount());
                pl.setSecondaryOrderCount(task.getSecondaryOrderCount());
                pl.setExceptionReason(task.getExceptionReason());
                pl.setPickMode(task.getTaskType());
                pl.setPickerName(task.getPickerName());
                for (WmsOutboundPickTaskOrder relation : pickTaskOrderMapper.selectByTaskId(task.getId())) {
                    taskOutbounds.add(toTaskOutboundVO(relation));
                }
                List<WmsOutboundPickTaskLine> taskLines = pickTaskLineMapper.selectByTaskId(task.getId());
                for (WmsOutboundPickTaskLine line : taskLines) {
                    lines.add(toAllocationVO(line));
                }
                int planned = taskLines.stream().mapToInt(line -> nvl(line.getPlannedQty())).sum();
                int picked = taskLines.stream().mapToInt(line -> nvl(line.getPickedQty())).sum();
                pl.setPlannedQuantity(planned);
                pl.setPickedQuantity(picked);
                pl.setCompletable("PICKING".equals(task.getTaskStatus()) && isTaskCompletable(taskLines));
            }
        }
        if (lines.isEmpty()) {
            for (WmsOutboundPickAllocation a : allocs) {
                lines.add(toAllocationVO(a));
            }
            pl.setCompletable(Boolean.FALSE);
        }
        if (taskOutbounds.isEmpty()) {
            PickTaskOutboundVO outbound = new PickTaskOutboundVO();
            outbound.setOutboundOrderId(vo.getId());
            outbound.setOutboundNo(vo.getOutboundNo());
            outbound.setSalesOrderCount(nvl(vo.getSalesOrderCount()));
            outbound.setSkuCount(nvl(vo.getSkuKinds()));
            outbound.setTotalQuantity(nvl(vo.getTotalQty()));
            outbound.setSortRequired(Boolean.FALSE);
            taskOutbounds.add(outbound);
            pl.setOutboundOrderCount(1);
            pl.setSalesOrderCount(nvl(vo.getSalesOrderCount()));
        }
        pl.setOutboundOrders(taskOutbounds);
        pl.setAllocations(lines);
        return pl;
    }

    public List<PickerVO> listPickers() {
        return outboundPickingMapper.selectPickers(currentPlatformTenantId());
    }

    // ==================== 下架（FIFO 锁定，防超卖 C7） ====================

    /**
     * 确认下架：两阶段（先锁批次算全量分配，全部够才应用）。
     * 任一 SKU 不足 → 整单挂起 BACKORDER（不部分下架），返回失败提示；成功 → 转 PICKING。
     */
    @Transactional(rollbackFor = Exception.class)
    public PickResult pick(PickDTO dto) {
        BatchPickDTO batch = new BatchPickDTO();
        batch.setOutboundOrderIds(Collections.singletonList(dto.getOutboundOrderId()));
        batch.setPickerId(dto.getPickerId());
        batch.setMaxOrdersPerTask(1);
        batch.setWholePalletPriority(Boolean.TRUE);
        createTasks(batch, true);
        return PickResult.ok();
    }

    public BatchPickPreviewVO previewBatch(BatchPickPreviewDTO dto) {
        assertPlatform();
        List<SalesOutboundOrder> orders = loadAndValidateOrders(dto.getOutboundOrderIds(), false, false);
        return buildBatchPreview(orders, safeMax(dto.getMaxOrdersPerTask()),
                !Boolean.FALSE.equals(dto.getWholePalletPriority()));
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchPickResultVO createBatch(BatchPickDTO dto) {
        return createTasks(dto, false);
    }

    private BatchPickResultVO createTasks(BatchPickDTO dto, boolean allowBackorder) {
        Long platformTenantId = currentPlatformTenantId();
        String pickerName = outboundPickingMapper.selectPickerName(dto.getPickerId(), platformTenantId);
        Assert.hasText(pickerName, "拣货员不存在、已停用或不属于当前海外仓平台");

        List<SalesOutboundOrder> orders = loadAndValidateOrders(dto.getOutboundOrderIds(), true, allowBackorder);
        for (SalesOutboundOrder order : orders) {
            ensureReservation(order);
        }
        List<List<SalesOutboundOrder>> groups = splitGroups(orders, safeMax(dto.getMaxOrdersPerTask()));
        BatchPickResultVO result = new BatchPickResultVO();
        List<Long> taskIds = new ArrayList<>();
        List<String> taskNos = new ArrayList<>();
        boolean wholePalletPriority = !Boolean.FALSE.equals(dto.getWholePalletPriority());

        for (List<SalesOutboundOrder> group : groups) {
            TaskAnalysis analysis = analyze(group, wholePalletPriority);
            WmsOutboundPickTask task = new WmsOutboundPickTask();
            SalesOutboundOrder first = group.get(0);
            task.setTaskNo(newTaskNo());
            task.setPlatformTenantId(platformTenantId);
            task.setWarehouseId(first.getWarehouseId());
            task.setErpTenantId(first.getErpTenantId());
            task.setSourceType(first.getSourceType());
            task.setTaskType(group.size() == 1 ? "SINGLE" : "WAVE");
            task.setTaskStatus("PICKING");
            task.setPickerId(dto.getPickerId());
            task.setPickerName(pickerName);
            task.setOrderCount(group.size());
            task.setSalesOrderCount(salesOrderCount(group));
            task.setSkuCount(analysis.skuCodes.size());
            task.setTotalQuantity(group.stream().mapToInt(o -> nvl(o.getTotalQuantity())).sum());
            task.setWholePalletCount(analysis.wholePalletIds.size());
            int packageCount = salesOrderCount(group);
            boolean taskNeedsSorting = packageCount > 1
                    && com.erp.admin.wms.model.enums.OutboundSourceType.SALES.name().equals(first.getSourceType());
            task.setSecondaryOrderCount(taskNeedsSorting ? packageCount : 0);
            pickTaskMapper.insert(task);

            int toteIndex = 1;
            for (SalesOutboundOrder order : group) {
                WmsOutboundPickTaskOrder relation = new WmsOutboundPickTaskOrder();
                relation.setTaskId(task.getId());
                relation.setOutboundOrderId(order.getId());
                relation.setOutboundNo(order.getOutboundNo());
				// 一个任务内只要有多个平台订单包裹，就必须逐包裹分货，不能只看单张出库单。
                boolean sorting = taskNeedsSorting
                        && com.erp.admin.wms.model.enums.OutboundSourceType.SALES.name().equals(order.getSourceType());
                relation.setSortRequired(sorting ? 1 : 0);
                relation.setSortStatus(sorting ? "PENDING" : "NOT_REQUIRED");
                relation.setToteNo(sorting ? String.format("B%02d", toteIndex++) : null);
                pickTaskOrderMapper.insert(relation);
				if (com.erp.admin.wms.model.enums.OutboundSourceType.SALES.name().equals(order.getSourceType())) {
					outboundPackageService.assignSortCodes(order.getId(), relation.getToteNo(), sorting);
				}

                order.setOrderStatus(OutboundOrderStatus.PICKING.name());
                order.setPickMode(task.getTaskType());
                order.setPickerId(dto.getPickerId());
                order.setPickerName(pickerName);
                int updated = salesOutboundMapper.updateById(order);
                if (updated != 1) {
                    throw new BusinessException(409, "出库单状态已变化，请刷新后重试: " + order.getOutboundNo());
                }
            }

            for (LineAccumulator acc : analysis.lines.values()) {
                WmsOutboundPickTaskLine line = new WmsOutboundPickTaskLine();
                line.setTaskId(task.getId());
                line.setPhysicalInventoryId(acc.batch.getId());
                line.setPalletId(acc.batch.getPalletId());
                if (acc.pallet != null) {
                    line.setPalletNo(acc.pallet.getPalletNo());
                    line.setSlotCode(acc.pallet.getSlotCode());
                }
                line.setLocationCode(acc.batch.getLocationCode());
                line.setSkuCode(acc.batch.getSkuCode());
                line.setInboundDate(acc.batch.getInboundDate());
                line.setPickOrder(acc.batch.getPickOrder());
                line.setPlannedQty(acc.qty);
                line.setPickedQty(0);
                line.setPickStrategy(acc.batch.getPalletId() != null
                        && analysis.wholePalletIds.contains(acc.batch.getPalletId()) ? "WHOLE_PALLET" : "PIECE");
                line.setLineStatus("PENDING");
                pickTaskLineMapper.insert(line);
            }
            taskIds.add(task.getId());
            taskNos.add(task.getTaskNo());
        }
        result.setTaskCount(taskIds.size());
        result.setOrderCount(orders.size());
        result.setSalesOrderCount(salesOrderCount(orders));
        result.setTaskIds(taskIds);
        result.setTaskNos(taskNos);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void scanPickLine(PickLineScanDTO dto, Long operatorId) {
        Long platformTenantId = currentPlatformTenantId();
        WmsOutboundPickTask task = pickTaskMapper.selectByIdForUpdate(dto.getTaskId());
        Assert.notNull(task, "拣货任务不存在");
        Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
        Assert.isTrue("PICKING".equals(task.getTaskStatus()), "当前任务不在正常拣货中");

        WmsOutboundPickTaskLine line = pickTaskLineMapper.selectByIdForUpdate(dto.getLineId());
        Assert.notNull(line, "拣货明细不存在");
        Assert.isTrue(Objects.equals(task.getId(), line.getTaskId()), "拣货明细不属于当前任务");
        Assert.isTrue(!"EXCEPTION".equals(line.getLineStatus()), "该明细存在异常，请先由主管处理");
        int remaining = nvl(line.getPlannedQty()) - nvl(line.getPickedQty());
        Assert.isTrue(remaining > 0, "该明细已经完成拣货");
        boolean manual = Boolean.TRUE.equals(dto.getManual());
        if (!manual) {
            Assert.hasText(dto.getLocationScanCode(), "请先扫描库位标签");
            Assert.isTrue(line.getLocationCode() != null
                            && line.getLocationCode().equalsIgnoreCase(dto.getLocationScanCode().trim()),
                    "扫描库位与任务要求不一致，应前往: " + line.getLocationCode());
        }
        boolean palletMatched = line.getPalletNo() != null
                && line.getPalletNo().equalsIgnoreCase(dto.getScanCode().trim());
        boolean skuMatched = skuBarcodeService.matches(task.getErpTenantId(), line.getSkuCode(), dto.getScanCode());
        Assert.isTrue(palletMatched || skuMatched,
                "扫描商品或托盘与任务明细不一致，要求SKU: " + line.getSkuCode());
        Assert.isTrue(dto.getQuantity() <= remaining,
                "实拣数量超过剩余计划，当前剩余: " + remaining);

        line.setPickedQty(nvl(line.getPickedQty()) + dto.getQuantity());
        line.setShortageQty(0);
        line.setExceptionReason(null);
        line.setLineStatus(nvl(line.getPickedQty()) == nvl(line.getPlannedQty())
                ? "COMPLETED" : "IN_PROGRESS");
        Assert.isTrue(pickTaskLineMapper.updateById(line) == 1, "拣货明细被其他操作更新，请刷新重试");
        recordScanEvent(task, line, null, "PICK", manual ? "MANUAL" : "SCAN",
                dto.getScanCode(), line.getSkuCode(), dto.getQuantity(), operatorId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reportPickException(PickExceptionDTO dto, Long operatorId) {
        Long platformTenantId = currentPlatformTenantId();
        WmsOutboundPickTask task = pickTaskMapper.selectByIdForUpdate(dto.getTaskId());
        Assert.notNull(task, "拣货任务不存在");
        Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
        Assert.isTrue("PICKING".equals(task.getTaskStatus()), "仅拣货中的任务可以报告异常");
        WmsOutboundPickTaskLine line = pickTaskLineMapper.selectByIdForUpdate(dto.getLineId());
        Assert.notNull(line, "拣货明细不存在");
        Assert.isTrue(Objects.equals(task.getId(), line.getTaskId()), "拣货明细不属于当前任务");
        int remaining = nvl(line.getPlannedQty()) - nvl(line.getPickedQty());
        Assert.isTrue(dto.getShortageQty() <= remaining, "缺货数量不能超过未拣数量");

        line.setShortageQty(dto.getShortageQty());
        line.setExceptionReason(dto.getReason());
        line.setLineStatus("EXCEPTION");
        Assert.isTrue(pickTaskLineMapper.updateById(line) == 1, "拣货异常登记冲突，请重试");
        task.setTaskStatus("EXCEPTION");
        task.setExceptionLineId(line.getId());
        task.setExceptionReason(dto.getReason());
        task.setExceptionTime(LocalDateTime.now());
        Assert.isTrue(pickTaskMapper.updateById(task) == 1, "拣货任务异常状态更新失败");
        recordScanEvent(task, line, null, "PICK", "EXCEPTION", null, line.getSkuCode(),
                dto.getShortageQty(), operatorId, dto.getReason());
    }

    public List<PickAllocationVO> listPickAlternatives(Long taskId, Long lineId) {
        Long platformTenantId = currentPlatformTenantId();
        WmsOutboundPickTask task = pickTaskMapper.selectById(taskId);
        Assert.notNull(task, "拣货任务不存在");
        Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
        WmsOutboundPickTaskLine line = pickTaskLineMapper.selectById(lineId);
        Assert.notNull(line, "拣货明细不存在");
        Assert.isTrue(Objects.equals(taskId, line.getTaskId()), "拣货明细不属于当前任务");
        int required = Math.max(nvl(line.getShortageQty()),
                nvl(line.getPlannedQty()) - nvl(line.getPickedQty()));
        return physicalInventoryMapper.selectFifoAllocatable(task.getErpTenantId(),
                        task.getWarehouseId(), line.getSkuCode()).stream()
                .filter(batch -> !Objects.equals(batch.getId(), line.getPhysicalInventoryId()))
                .filter(batch -> nvl(batch.getQuantity()) - nvl(batch.getReservedQty()) > 0)
                .map(batch -> toAllocationVO(line.getSkuCode(), batch,
                        Math.min(required, nvl(batch.getQuantity()) - nvl(batch.getReservedQty()))))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolvePickException(ResolvePickExceptionDTO dto, Long operatorId) {
        Long platformTenantId = currentPlatformTenantId();
        WmsOutboundPickTask task = pickTaskMapper.selectByIdForUpdate(dto.getTaskId());
        Assert.notNull(task, "拣货任务不存在");
        Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
        Assert.isTrue("EXCEPTION".equals(task.getTaskStatus()), "当前任务没有待处理异常");
        WmsOutboundPickTaskLine line = pickTaskLineMapper.selectByIdForUpdate(task.getExceptionLineId());
        Assert.notNull(line, "异常拣货明细不存在");
        String action = dto.getAction().trim().toUpperCase();
        if ("RETRY".equals(action)) {
            line.setLineStatus(nvl(line.getPickedQty()) > 0 ? "IN_PROGRESS" : "PENDING");
            line.setShortageQty(0);
            line.setExceptionReason(null);
            Assert.isTrue(pickTaskLineMapper.updateById(line) == 1, "拣货明细恢复失败");
            resumeTask(task);
        }
        else if ("REALLOCATE".equals(action)) {
            Assert.notNull(dto.getReplacementInventoryId(), "请选择替代库存批次");
            reallocatePickLine(task, line, dto.getReplacementInventoryId());
            resumeTask(task);
        }
        else if ("SHORT_CLOSE".equals(action)) {
            closeShortTask(task);
        }
        else {
            throw new BusinessException(400, "不支持的异常处理方式: " + dto.getAction());
        }
        recordScanEvent(task, line, null, "PICK", "RESOLVE", null, line.getSkuCode(), 0,
                operatorId, action + (dto.getRemark() == null ? "" : ": " + dto.getRemark()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId) {
        Long platformTenantId = currentPlatformTenantId();
        WmsOutboundPickTask task = pickTaskMapper.selectByIdForUpdate(taskId);
        Assert.notNull(task, "拣货任务不存在");
        Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
        Assert.isTrue("PICKING".equals(task.getTaskStatus()), "仅拣货中的任务可以完成");
        List<WmsOutboundPickTaskLine> lines = pickTaskLineMapper.selectByTaskId(taskId);
        Assert.notEmpty(lines, "拣货任务没有取货明细");
        Assert.isTrue(isTaskCompletable(lines), "任务仍有未完成或异常的拣货明细");
        for (WmsOutboundPickTaskLine line : lines) {
            Assert.isTrue(nvl(line.getPickedQty()) == nvl(line.getPlannedQty()),
                    "SKU[" + line.getSkuCode() + "]位于[" + line.getLocationCode()
                            + "]的实拣数量未完成，计划" + nvl(line.getPlannedQty())
                            + "，已拣" + nvl(line.getPickedQty()));
            Assert.isTrue(!"EXCEPTION".equals(line.getLineStatus()), "任务仍有未处理的拣货异常");
        }
		boolean hasSorting = false;
        for (WmsOutboundPickTaskOrder relation : pickTaskOrderMapper.selectByTaskId(taskId)) {
            SalesOutboundOrder order = salesOutboundMapper.selectByIdForUpdate(relation.getOutboundOrderId());
            Assert.notNull(order, "出库单不存在: " + relation.getOutboundNo());
            Assert.isTrue(OutboundOrderStatus.PICKING.name().equals(order.getOrderStatus()),
                    "出库单不在拣货中: " + relation.getOutboundNo());
            if (relation.getSortRequired() != null && relation.getSortRequired() == 1) {
				hasSorting = true;
				relation.setSortStatus("PENDING");
				Assert.isTrue(pickTaskOrderMapper.updateById(relation) == 1, "分货任务状态更新失败");
			}
			else {
				order.setOrderStatus(OutboundOrderStatus.PICKED.name());
				Assert.isTrue(salesOutboundMapper.updateById(order) == 1, "出库单状态更新失败");
            }
        }
		task.setTaskStatus(hasSorting ? "SORTING" : "COMPLETED");
        pickTaskMapper.updateById(task);
    }

	@Transactional(rollbackFor = Exception.class)
	public void scanPackageSort(Long taskId, PackageScanDTO dto, Long operatorId) {
		Long platformTenantId = currentPlatformTenantId();
		WmsOutboundPickTask task = pickTaskMapper.selectByIdForUpdate(taskId);
		Assert.notNull(task, "拣货任务不存在");
		Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
		Assert.isTrue("SORTING".equals(task.getTaskStatus()), "当前拣货任务不在分货中");
		boolean belongs = pickTaskOrderMapper.selectByTaskId(taskId).stream()
				.anyMatch(relation -> Objects.equals(relation.getOutboundOrderId(), dto.getOutboundOrderId()));
		Assert.isTrue(belongs, "该出库单不属于当前拣货任务");
		boolean packageBelongs = outboundPackageService.listEntities(dto.getOutboundOrderId()).stream()
				.anyMatch(pack -> Objects.equals(pack.getId(), dto.getPackageId()));
		Assert.isTrue(packageBelongs, "该平台订单包裹不属于当前拣货任务");
		outboundPackageService.scanSort(dto.getPackageId(), dto.getScanCode(), dto.getQuantity(),
				Boolean.TRUE.equals(dto.getManual()), operatorId, task.getPickerName());
	}

	/** 确认一个平台订单格口已完成分货；全部完成后自动进入待打包。 */
	@Transactional(rollbackFor = Exception.class)
	public void confirmPackageSort(Long taskId, Long packageId) {
		Long platformTenantId = currentPlatformTenantId();
		WmsOutboundPickTask task = pickTaskMapper.selectByIdForUpdate(taskId);
		Assert.notNull(task, "拣货任务不存在");
		Assert.isTrue(Objects.equals(platformTenantId, task.getPlatformTenantId()), "无权操作该拣货任务");
		Assert.isTrue("SORTING".equals(task.getTaskStatus()), "当前拣货任务不在分货中");

		com.erp.admin.wms.model.entity.WmsSalesOutboundPackage pack = outboundPackageService
				.confirmSorted(packageId, task.getPickerName());
		WmsOutboundPickTaskOrder targetRelation = pickTaskOrderMapper.selectByTaskId(taskId).stream()
				.filter(relation -> Objects.equals(relation.getOutboundOrderId(), pack.getOutboundOrderId()))
				.findFirst().orElse(null);
		Assert.notNull(targetRelation, "该平台订单不属于当前拣货任务");
		if (outboundPackageService.allSorted(pack.getOutboundOrderId())) {
			SalesOutboundOrder order = salesOutboundMapper.selectByIdForUpdate(pack.getOutboundOrderId());
			Assert.notNull(order, "出库单不存在");
			if (OutboundOrderStatus.PICKING.name().equals(order.getOrderStatus())) {
				order.setOrderStatus(OutboundOrderStatus.PICKED.name());
				Assert.isTrue(salesOutboundMapper.updateById(order) == 1, "出库单状态更新失败");
			}
			targetRelation.setSortStatus("COMPLETED");
			Assert.isTrue(pickTaskOrderMapper.updateById(targetRelation) == 1, "分货任务状态更新失败");
		}

		boolean allDone = true;
		for (WmsOutboundPickTaskOrder relation : pickTaskOrderMapper.selectByTaskId(taskId)) {
			SalesOutboundOrder order = salesOutboundMapper.selectById(relation.getOutboundOrderId());
			if (order == null || !OutboundOrderStatus.PICKED.name().equals(order.getOrderStatus())) {
				allDone = false;
				break;
			}
		}
		if (allDone) {
			task.setTaskStatus("COMPLETED");
			Assert.isTrue(pickTaskMapper.updateById(task) == 1, "拣货任务完成状态更新失败");
		}
	}

	private void resumeTask(WmsOutboundPickTask task) {
		task.setTaskStatus("PICKING");
		task.setExceptionLineId(null);
		task.setExceptionReason(null);
		task.setExceptionTime(null);
		Assert.isTrue(pickTaskMapper.updateById(task) == 1, "拣货任务恢复失败");
	}

	private void reallocatePickLine(WmsOutboundPickTask task, WmsOutboundPickTaskLine sourceLine,
			Long replacementInventoryId) {
		int remaining = nvl(sourceLine.getPlannedQty()) - nvl(sourceLine.getPickedQty());
		int moveQty = nvl(sourceLine.getShortageQty()) > 0
				? Math.min(nvl(sourceLine.getShortageQty()), remaining) : remaining;
		Assert.isTrue(moveQty > 0, "该异常明细没有需要重新分配的数量");

		WmsPhysicalInventory sourceBatch = physicalInventoryMapper
				.selectByIdForUpdate(sourceLine.getPhysicalInventoryId());
		WmsPhysicalInventory targetBatch = physicalInventoryMapper.selectByIdForUpdate(replacementInventoryId);
		Assert.notNull(sourceBatch, "原库存批次不存在");
		Assert.notNull(targetBatch, "替代库存批次不存在");
		Assert.isTrue(!Objects.equals(sourceBatch.getId(), targetBatch.getId()), "替代批次不能与原批次相同");
		Assert.isTrue(Objects.equals(task.getErpTenantId(), targetBatch.getErpTenantId())
						&& Objects.equals(task.getWarehouseId(), targetBatch.getWarehouseId()),
				"替代批次不属于当前货主和仓库");
		Assert.isTrue(Objects.equals(sourceLine.getSkuCode(), targetBatch.getSkuCode()),
				"替代批次SKU不一致");
		Assert.isTrue("GOOD".equals(targetBatch.getQuality())
						&& Integer.valueOf(1).equals(targetBatch.getAllocatable())
						&& !Integer.valueOf(1).equals(targetBatch.getContainerStored()),
				"替代批次当前不可分配");
		int targetAvailable = nvl(targetBatch.getQuantity()) - nvl(targetBatch.getReservedQty());
		Assert.isTrue(targetAvailable >= moveQty,
				"替代批次可分配数量不足，需要" + moveQty + "，当前" + targetAvailable);
		Assert.isTrue(nvl(sourceBatch.getReservedQty()) >= moveQty, "原批次预留数量异常");

		sourceBatch.setReservedQty(nvl(sourceBatch.getReservedQty()) - moveQty);
		targetBatch.setReservedQty(nvl(targetBatch.getReservedQty()) + moveQty);
		Assert.isTrue(physicalInventoryMapper.updateById(sourceBatch) == 1, "原批次库存版本冲突，请重试");
		Assert.isTrue(physicalInventoryMapper.updateById(targetBatch) == 1, "替代批次库存版本冲突，请重试");

		List<Long> orderIds = pickTaskOrderMapper.selectByTaskId(task.getId()).stream()
				.map(WmsOutboundPickTaskOrder::getOutboundOrderId).collect(Collectors.toList());
		List<WmsOutboundPickAllocation> allocations = pickAllocationMapper
				.selectByOrdersAndInventory(orderIds, sourceBatch.getId());
		int toMove = moveQty;
		for (WmsOutboundPickAllocation allocation : allocations) {
			if (toMove <= 0) {
				break;
			}
			int allocationQty = nvl(allocation.getTakeQty());
			int allocationMove = Math.min(toMove, allocationQty);
			if (allocationMove == allocationQty) {
				fillAllocationBatch(allocation, targetBatch);
				Assert.isTrue(pickAllocationMapper.updateById(allocation) == 1, "出库预留重分配失败");
			}
			else {
				allocation.setTakeQty(allocationQty - allocationMove);
				Assert.isTrue(pickAllocationMapper.updateById(allocation) == 1, "原出库预留拆分失败");
				WmsOutboundPickAllocation replacement = new WmsOutboundPickAllocation();
				replacement.setOutboundOrderId(allocation.getOutboundOrderId());
				replacement.setTakeQty(allocationMove);
				fillAllocationBatch(replacement, targetBatch);
				pickAllocationMapper.insert(replacement);
			}
			toMove -= allocationMove;
		}
		Assert.isTrue(toMove == 0, "出库预留记录不足，无法完成批次重分配");

		sourceLine.setPlannedQty(nvl(sourceLine.getPlannedQty()) - moveQty);
		sourceLine.setShortageQty(0);
		sourceLine.setExceptionReason(null);
		sourceLine.setLineStatus(nvl(sourceLine.getPlannedQty()) == nvl(sourceLine.getPickedQty())
				? "COMPLETED" : "IN_PROGRESS");
		Assert.isTrue(pickTaskLineMapper.updateById(sourceLine) == 1, "原拣货明细更新失败");

		WmsOutboundPickTaskLine targetLine = pickTaskLineMapper.selectByTaskId(task.getId()).stream()
				.filter(item -> Objects.equals(item.getPhysicalInventoryId(), targetBatch.getId()))
				.findFirst().orElse(null);
		if (targetLine == null) {
			targetLine = new WmsOutboundPickTaskLine();
			targetLine.setTaskId(task.getId());
			targetLine.setPhysicalInventoryId(targetBatch.getId());
			targetLine.setPalletId(targetBatch.getPalletId());
			if (targetBatch.getPalletId() != null) {
				WmsPallet pallet = palletMapper.selectById(targetBatch.getPalletId());
				if (pallet != null) {
					targetLine.setPalletNo(pallet.getPalletNo());
					targetLine.setSlotCode(pallet.getSlotCode());
				}
			}
			targetLine.setLocationCode(targetBatch.getLocationCode());
			targetLine.setSkuCode(targetBatch.getSkuCode());
			targetLine.setInboundDate(targetBatch.getInboundDate());
			targetLine.setPickOrder(targetBatch.getPickOrder());
			targetLine.setPlannedQty(moveQty);
			targetLine.setPickedQty(0);
			targetLine.setShortageQty(0);
			targetLine.setPickStrategy("PIECE");
			targetLine.setLineStatus("PENDING");
			pickTaskLineMapper.insert(targetLine);
		}
		else {
			targetLine = pickTaskLineMapper.selectByIdForUpdate(targetLine.getId());
			targetLine.setPlannedQty(nvl(targetLine.getPlannedQty()) + moveQty);
			targetLine.setLineStatus(nvl(targetLine.getPickedQty()) == nvl(targetLine.getPlannedQty())
					? "COMPLETED" : "IN_PROGRESS");
			Assert.isTrue(pickTaskLineMapper.updateById(targetLine) == 1, "替代拣货明细更新失败");
		}

		inventoryAggregator.refreshSnapshot(0L, task.getErpTenantId(), task.getWarehouseId(), sourceLine.getSkuCode());
		if (sourceBatch.getPalletId() != null) {
			palletService.refreshAfterInventoryChange(sourceBatch.getPalletId());
		}
		if (targetBatch.getPalletId() != null && !Objects.equals(sourceBatch.getPalletId(), targetBatch.getPalletId())) {
			palletService.refreshAfterInventoryChange(targetBatch.getPalletId());
		}
	}

	private static void fillAllocationBatch(WmsOutboundPickAllocation allocation, WmsPhysicalInventory batch) {
		allocation.setPhysicalInventoryId(batch.getId());
		allocation.setSkuCode(batch.getSkuCode());
		allocation.setLocationCode(batch.getLocationCode());
		allocation.setInboundDate(batch.getInboundDate());
		allocation.setPickOrder(batch.getPickOrder());
	}

	private void closeShortTask(WmsOutboundPickTask task) {
		for (WmsOutboundPickTaskOrder relation : pickTaskOrderMapper.selectByTaskId(task.getId())) {
			SalesOutboundOrder order = salesOutboundMapper.selectByIdForUpdate(relation.getOutboundOrderId());
			if (order != null) {
				releaseForOrder(order);
				order.setOrderStatus(OutboundOrderStatus.BACKORDER.name());
				Assert.isTrue(salesOutboundMapper.updateById(order) == 1, "缺货出库单状态更新失败");
			}
		}
		task.setTaskStatus("CANCELLED");
		Assert.isTrue(pickTaskMapper.updateById(task) == 1, "缺货任务关闭失败");
	}

	private void recordScanEvent(WmsOutboundPickTask task, WmsOutboundPickTaskLine line,
			Long packageId, String stage, String eventType, String scanCode, String skuCode,
			Integer quantity, Long operatorId, String remark) {
		WmsOutboundScanEvent event = new WmsOutboundScanEvent();
		event.setTaskId(task == null ? null : task.getId());
		event.setTaskLineId(line == null ? null : line.getId());
		event.setPackageId(packageId);
		event.setStage(stage);
		event.setEventType(eventType);
		event.setScanCode(scanCode);
		event.setSkuCode(skuCode);
		event.setQuantity(quantity == null ? 0 : quantity);
		event.setOperatorId(operatorId);
		event.setOperatorName(task == null ? null : task.getPickerName());
		event.setRemark(remark);
		scanEventMapper.insert(event);
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
                    .selectFifoAllocatableForUpdate(order.getErpTenantId(), order.getWarehouseId(), e.getKey());
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
        applyReservations(order, planned);
        return java.util.Collections.emptyList();
    }

    /**
     * Reserves every currently pickable physical batch and returns the remaining
     * shortage. Used when the shortage can be supplied from a virtual location.
     */
    public List<StockShortageVO> reserveAvailableForOrder(SalesOutboundOrder order,
            List<SalesOutboundOrderItem> items) {
        return reserveUpToRequired(order, requiredBySku(items), Collections.emptyMap());
    }

    /**
     * Completes an existing partial reservation after a location adjustment has
     * moved the planned virtual stock into physical locations.
     */
    public List<StockShortageVO> reserveMissingForOrder(SalesOutboundOrder order,
            List<SalesOutboundOrderItem> items) {
        Map<String, Integer> alreadyReserved = pickAllocationMapper.selectByOutboundOrderId(order.getId()).stream()
                .collect(Collectors.groupingBy(WmsOutboundPickAllocation::getSkuCode, LinkedHashMap::new,
                        Collectors.summingInt(a -> nvl(a.getTakeQty()))));
        return reserveUpToRequired(order, requiredBySku(items), alreadyReserved);
    }

    private List<StockShortageVO> reserveUpToRequired(SalesOutboundOrder order, Map<String, Integer> required,
            Map<String, Integer> alreadyReserved) {
        List<Take> planned = new ArrayList<>();
        List<StockShortageVO> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int needed = Math.max(entry.getValue() - alreadyReserved.getOrDefault(entry.getKey(), 0), 0);
            if (needed == 0) {
                continue;
            }
            List<WmsPhysicalInventory> batches = physicalInventoryMapper.selectFifoAllocatableForUpdate(
                    order.getErpTenantId(), order.getWarehouseId(), entry.getKey());
            preferLoosePallets(batches);
            AllocPlan plan = planAllocation(batches, needed);
            planned.addAll(plan.takes);
            if (plan.shortage > 0) {
                StockShortageVO shortage = new StockShortageVO();
                shortage.setSkuCode(entry.getKey());
                shortage.setSkuName(entry.getKey());
                shortage.setRequiredQty(entry.getValue());
                shortage.setAvailableQty(entry.getValue() - plan.shortage);
                shortage.setShortage(plan.shortage);
                shortages.add(shortage);
            }
        }
        applyReservations(order, planned);
        return shortages;
    }

    private void applyReservations(SalesOutboundOrder order, List<Take> planned) {
        Set<String> touchedSku = new LinkedHashSet<>();
        Set<Long> touchedPallets = new LinkedHashSet<>();
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

    private List<SalesOutboundOrder> loadAndValidateOrders(List<Long> ids, boolean lock, boolean allowBackorder) {
        Assert.notEmpty(ids, "请选择待下架订单");
        List<Long> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().sorted()
                .collect(Collectors.toList());
        Assert.isTrue(!distinctIds.isEmpty() && distinctIds.size() <= 500, "每次最多选择500张订单");
        List<SalesOutboundOrder> orders = new ArrayList<>();
        for (Long id : distinctIds) {
            SalesOutboundOrder order = lock ? salesOutboundMapper.selectByIdForUpdate(id) : salesOutboundMapper.selectById(id);
            Assert.notNull(order, "出库单不存在: " + id);
            boolean valid = OutboundOrderStatus.CONFIRMED.name().equals(order.getOrderStatus())
                    || (allowBackorder && OutboundOrderStatus.BACKORDER.name().equals(order.getOrderStatus()));
            Assert.isTrue(valid, "仅待下架订单可以创建拣货任务: " + order.getOutboundNo());
            orders.add(order);
        }
        return orders;
    }

    private void ensureReservation(SalesOutboundOrder order) {
        List<WmsOutboundPickAllocation> allocations = pickAllocationMapper.selectByOutboundOrderId(order.getId());
        if (allocations.isEmpty()) {
            List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(order.getId());
            Assert.notEmpty(items, "出库单无明细: " + order.getOutboundNo());
            List<StockShortageVO> shortages = reserveForOrder(order, items);
            if (!shortages.isEmpty()) {
                throw new BusinessException(40030, "SKU[" + shortages.get(0).getSkuCode()
                        + "] 可用良品不足，无法创建拣货任务");
            }
            allocations = pickAllocationMapper.selectByOutboundOrderId(order.getId());
        }
        Map<String, Integer> reserved = allocations.stream().collect(Collectors.groupingBy(
                WmsOutboundPickAllocation::getSkuCode, LinkedHashMap::new,
                Collectors.summingInt(a -> nvl(a.getTakeQty()))));
        Map<String, Integer> required = requiredBySku(salesOutboundItemMapper.selectByOutboundOrderId(order.getId()));
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            Assert.isTrue(reserved.getOrDefault(entry.getKey(), 0) >= entry.getValue(),
                    "订单预留不完整，请取消确认后重新确认: " + order.getOutboundNo() + "/" + entry.getKey());
        }
    }

    private BatchPickPreviewVO buildBatchPreview(List<SalesOutboundOrder> orders, int maxOrders, boolean wholePriority) {
        List<PickTaskPreviewVO> tasks = new ArrayList<>();
        for (List<SalesOutboundOrder> group : splitGroups(orders, maxOrders)) {
            TaskAnalysis analysis = analyze(group, wholePriority);
            SalesOutboundOrder first = group.get(0);
            OutboundOrderVO names = outboundPickingMapper.selectOrderById(first.getId());
            PickTaskPreviewVO vo = new PickTaskPreviewVO();
            vo.setWarehouseId(first.getWarehouseId());
            vo.setWarehouseName(names == null ? null : names.getWarehouseName());
            vo.setErpTenantId(first.getErpTenantId());
            vo.setOwnerName(names == null ? null : names.getOwnerName());
            vo.setSourceType(first.getSourceType());
            vo.setTaskType(group.size() == 1 ? "SINGLE" : "WAVE");
            vo.setOutboundOrderIds(group.stream().map(SalesOutboundOrder::getId).collect(Collectors.toList()));
            vo.setOrderCount(group.size());
            vo.setSalesOrderCount(salesOrderCount(group));
            vo.setSkuCount(analysis.skuCodes.size());
            vo.setTotalQuantity(group.stream().mapToInt(o -> nvl(o.getTotalQuantity())).sum());
            vo.setWholePalletCount(analysis.wholePalletIds.size());
            int packageCount = salesOrderCount(group);
            vo.setSecondaryOrderCount(com.erp.admin.wms.model.enums.OutboundSourceType.SALES.name()
                    .equals(first.getSourceType()) && packageCount > 1 ? packageCount : 0);
            tasks.add(vo);
        }
        BatchPickPreviewVO result = new BatchPickPreviewVO();
        result.setSelectedOrderCount(orders.size());
        result.setSelectedSalesOrderCount(salesOrderCount(orders));
        result.setTaskCount(tasks.size());
        result.setTotalQuantity(tasks.stream().mapToInt(t -> nvl(t.getTotalQuantity())).sum());
        result.setWholePalletCount(tasks.stream().mapToInt(t -> nvl(t.getWholePalletCount())).sum());
        result.setSecondaryOrderCount(tasks.stream().mapToInt(t -> nvl(t.getSecondaryOrderCount())).sum());
        result.setTasks(tasks);
        return result;
    }

    private List<List<SalesOutboundOrder>> splitGroups(List<SalesOutboundOrder> orders, int maxOrders) {
        Map<String, List<SalesOutboundOrder>> grouped = new LinkedHashMap<>();
        for (SalesOutboundOrder order : orders) {
            String key = order.getWarehouseId() + "|" + order.getErpTenantId() + "|" + order.getSourceType();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(order);
        }
        List<List<SalesOutboundOrder>> result = new ArrayList<>();
        for (List<SalesOutboundOrder> group : grouped.values()) {
            group.sort(Comparator.comparing(SalesOutboundOrder::getId));
            for (int from = 0; from < group.size(); from += maxOrders) {
                result.add(new ArrayList<>(group.subList(from, Math.min(group.size(), from + maxOrders))));
            }
        }
        return result;
    }

    private TaskAnalysis analyze(List<SalesOutboundOrder> orders, boolean wholePriority) {
        TaskAnalysis result = new TaskAnalysis();
        Map<Long, Integer> allocatedByPallet = new HashMap<>();
        Map<Long, WmsPallet> pallets = new HashMap<>();
        for (SalesOutboundOrder order : orders) {
            for (WmsOutboundPickAllocation allocation : pickAllocationMapper.selectByOutboundOrderId(order.getId())) {
                WmsPhysicalInventory batch = physicalInventoryMapper.selectById(allocation.getPhysicalInventoryId());
                Assert.notNull(batch, "预留批次不存在: " + allocation.getPhysicalInventoryId());
                LineAccumulator acc = result.lines.computeIfAbsent(batch.getId(), id -> new LineAccumulator(batch));
                acc.qty += nvl(allocation.getTakeQty());
                result.skuCodes.add(batch.getSkuCode());
                if (batch.getPalletId() != null) {
                    allocatedByPallet.merge(batch.getPalletId(), nvl(allocation.getTakeQty()), Integer::sum);
                    pallets.computeIfAbsent(batch.getPalletId(), palletMapper::selectById);
                    acc.pallet = pallets.get(batch.getPalletId());
                }
            }
        }
        if (wholePriority) {
            for (Map.Entry<Long, Integer> entry : allocatedByPallet.entrySet()) {
                WmsPallet pallet = pallets.get(entry.getKey());
                if (pallet == null || pallet.getCurrentSlotId() == null
                        || "LOCKED".equals(pallet.getPalletStatus())) {
                    continue;
                }
                int palletQty = physicalInventoryMapper.listByPalletId(entry.getKey()).stream()
                        .mapToInt(b -> nvl(b.getQuantity())).sum();
                if (palletQty > 0 && entry.getValue() == palletQty) {
                    result.wholePalletIds.add(entry.getKey());
                }
            }
        }
        return result;
    }

    private int safeMax(Integer value) {
        return value == null ? 20 : Math.max(1, Math.min(value, 50));
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    /** 销售出库单关联的电商平台订单总数；自定义出库的 orderCount 为 0。 */
    public static int salesOrderCount(List<SalesOutboundOrder> orders) {
        return orders == null ? 0 : orders.stream().mapToInt(o -> nvl(o.getOrderCount())).sum();
    }

    public static boolean isTaskCompletable(List<WmsOutboundPickTaskLine> lines) {
        return lines != null && !lines.isEmpty()
                && lines.stream().allMatch(line -> nvl(line.getPickedQty()) == nvl(line.getPlannedQty())
                        && !"EXCEPTION".equals(line.getLineStatus()));
    }

    private String newTaskNo() {
        return "PT" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void attachTask(OutboundOrderVO order) {
        WmsOutboundPickTaskOrder relation = pickTaskOrderMapper.selectLatestByOrderId(order.getId());
        if (relation == null) {
            return;
        }
        WmsOutboundPickTask task = pickTaskMapper.selectById(relation.getTaskId());
        if (task != null && !"CANCELLED".equals(task.getTaskStatus())) {
            order.setPickTaskId(task.getId());
            order.setPickTaskNo(task.getTaskNo());
            order.setPickTaskOutboundOrderCount(task.getOrderCount());
            order.setPickTaskSalesOrderCount(task.getSalesOrderCount());
        }
    }

    private PickTaskOutboundVO toTaskOutboundVO(WmsOutboundPickTaskOrder relation) {
        PickTaskOutboundVO vo = new PickTaskOutboundVO();
        vo.setOutboundOrderId(relation.getOutboundOrderId());
        vo.setOutboundNo(relation.getOutboundNo());
        vo.setToteNo(relation.getToteNo());
        vo.setSortRequired(Integer.valueOf(1).equals(relation.getSortRequired()));
        SalesOutboundOrder order = salesOutboundMapper.selectById(relation.getOutboundOrderId());
        if (order != null) {
            vo.setSalesOrderCount(nvl(order.getOrderCount()));
            vo.setSkuCount(nvl(order.getSkuCount()));
            vo.setTotalQuantity(nvl(order.getTotalQuantity()));
        }
		vo.setPackages(outboundPackageService.listPackages(relation.getOutboundOrderId()));
        return vo;
    }

    private List<OutboundOrderItemVO> buildItemsWithAvailability(OutboundOrderVO order) {
        List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(order.getId());
        Map<String, Integer> required = requiredBySku(items);
        Map<String, Integer> ownReserved = pickAllocationMapper.selectByOutboundOrderId(order.getId()).stream()
                .collect(Collectors.groupingBy(WmsOutboundPickAllocation::getSkuCode, LinkedHashMap::new,
                        Collectors.summingInt(a -> nvl(a.getTakeQty()))));
        List<OutboundOrderItemVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : required.entrySet()) {
            List<WmsPhysicalInventory> batches = physicalInventoryMapper
					.selectFifoAllocatable(order.getErpTenantId(), order.getWarehouseId(), e.getKey());
            int publicAvailable = availableQty(batches);
            int reservedForOrder = ownReserved.getOrDefault(e.getKey(), 0);
            int available = reservedForOrder > 0 ? reservedForOrder : publicAvailable;
            OutboundOrderItemVO vo = new OutboundOrderItemVO();
            vo.setSkuCode(e.getKey());
            vo.setRequiredQty(e.getValue());
            vo.setAvailableQty(available);
            vo.setOwnReservedQty(reservedForOrder);
            vo.setPublicAvailableQty(publicAvailable);
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
        av.setPhysicalInventoryId(b.getId());
        av.setSkuCode(sku);
        av.setLocationCode(b.getLocationCode());
        av.setInboundDate(b.getInboundDate() == null ? null : b.getInboundDate().toString());
        av.setBatchNo(batchNo(b.getInboundDate() == null ? null : b.getInboundDate().toString(), b.getPickOrder()));
        av.setTakeQty(take);
        av.setPlannedQty(take);
        av.setPickedQty(0);
        av.setRemainingQty(take);
        av.setPalletId(b.getPalletId());
        if (b.getPalletId() != null) {
            WmsPallet pallet = palletMapper.selectById(b.getPalletId());
            if (pallet != null) {
                av.setPalletNo(pallet.getPalletNo());
                av.setSlotCode(pallet.getSlotCode());
            }
        }
        return av;
    }

    private PickAllocationVO toAllocationVO(WmsOutboundPickAllocation allocation) {
        WmsPhysicalInventory batch = physicalInventoryMapper.selectById(allocation.getPhysicalInventoryId());
        if (batch == null) {
            PickAllocationVO vo = new PickAllocationVO();
            vo.setSkuCode(allocation.getSkuCode());
            vo.setLocationCode(allocation.getLocationCode());
            vo.setInboundDate(allocation.getInboundDate() == null ? null : allocation.getInboundDate().toString());
            vo.setBatchNo(batchNo(vo.getInboundDate(), allocation.getPickOrder()));
            vo.setTakeQty(allocation.getTakeQty());
            return vo;
        }
        return toAllocationVO(allocation.getSkuCode(), batch, nvl(allocation.getTakeQty()));
    }

    private PickAllocationVO toAllocationVO(WmsOutboundPickTaskLine line) {
        PickAllocationVO vo = new PickAllocationVO();
        vo.setLineId(line.getId());
        vo.setPhysicalInventoryId(line.getPhysicalInventoryId());
        vo.setSkuCode(line.getSkuCode());
        vo.setLocationCode(line.getLocationCode());
        vo.setInboundDate(line.getInboundDate() == null ? null : line.getInboundDate().toString());
        vo.setBatchNo(batchNo(vo.getInboundDate(), line.getPickOrder()));
        vo.setTakeQty(line.getPlannedQty());
        vo.setPlannedQty(line.getPlannedQty());
        vo.setPickedQty(nvl(line.getPickedQty()));
        vo.setRemainingQty(Math.max(nvl(line.getPlannedQty()) - nvl(line.getPickedQty()), 0));
        vo.setShortageQty(nvl(line.getShortageQty()));
        vo.setLineStatus(line.getLineStatus());
        vo.setExceptionReason(line.getExceptionReason());
        vo.setPalletId(line.getPalletId());
        vo.setPalletNo(line.getPalletNo());
        vo.setSlotCode(line.getSlotCode());
        vo.setPickStrategy(line.getPickStrategy());
        return vo;
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
        currentPlatformTenantId();
    }

    private Long currentPlatformTenantId() {
        com.erp.admin.tenant.model.vo.TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identity.getIdentityType())) {
            throw new BusinessException(403, "仅海外仓平台可执行出库作业");
        }
        return identity.getTenantId();
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
            return Arrays.asList(OutboundOrderStatus.PICKED.name(), OutboundOrderStatus.PACKED.name(),
                    OutboundOrderStatus.SHIPPED.name(), OutboundOrderStatus.COMPLETED.name());
        }
        return java.util.Collections.singletonList(viewStatus);
    }

    // ---- 内部结构 ----

    private static class TaskAnalysis {
        final Map<Long, LineAccumulator> lines = new LinkedHashMap<>();
        final Set<String> skuCodes = new LinkedHashSet<>();
        final Set<Long> wholePalletIds = new LinkedHashSet<>();
    }

    private static class LineAccumulator {
        final WmsPhysicalInventory batch;
        WmsPallet pallet;
        int qty;

        LineAccumulator(WmsPhysicalInventory batch) {
            this.batch = batch;
        }
    }

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
