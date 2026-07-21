package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.OutboundShippingMapper;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.mapper.WmsClientBillingRecordMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickAllocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.dto.PackDTO;
import com.erp.admin.wms.model.dto.ShipDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsClientBillingRecord;
import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.qo.PackShipQO;
import com.erp.admin.wms.model.vo.LogisticsChannelVO;
import com.erp.admin.wms.model.vo.PackShipItemVO;
import com.erp.admin.wms.model.vo.PackShipOrderVO;
import com.erp.admin.wms.model.vo.ShipResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 海外仓平台出库作业·打包签出（业务需求 1.3.2）。基于既有销售出库单。
 *
 * <p>PICKING→(打包)→PACKED→(签出完成)→SHIPPED。SHIPPED 是本系统最终状态。
 * <b>签出才真正扣物理库存</b>：据下架生成的 FIFO 分配
 * （{@code wms_outbound_pick_allocation}）逐批次 {@code quantity -= take} 并释放 {@code reserved_qty}，
 * 同事务聚合刷新 {@code wms_inventory}。关联物流产品时生成客户计费流水（幂等 biz_id）。</p>
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundShippingService {

    /** 打包签出页作用域（DB 状态）：拣货中 + 已打包 + 已发货 + 完成 */
    private static final List<String> DEFAULT_SCOPE = Arrays.asList(
            OutboundOrderStatus.PICKING.name(),
            OutboundOrderStatus.PACKED.name(),
            OutboundOrderStatus.SHIPPED.name(),
            OutboundOrderStatus.COMPLETED.name());

    private final OutboundShippingMapper outboundShippingMapper;

    private final SalesOutboundMapper salesOutboundMapper;

    private final SalesOutboundItemMapper salesOutboundItemMapper;

    private final WmsPhysicalInventoryMapper physicalInventoryMapper;

    private final WmsOutboundPickAllocationMapper pickAllocationMapper;

    private final WmsClientBillingRecordMapper clientBillingRecordMapper;

    private final WmsInventoryAggregator inventoryAggregator;

    private final WmsLogisticsProductService logisticsProductService;

    private final com.erp.admin.tenant.mapper.SysTenantMapper sysTenantMapper;

    private final TenantIdentityService tenantIdentityService;

    private final WmsPalletService palletService;

    // ==================== 查询 ====================

    public PageResult<PackShipOrderVO> page(PageParam pageParam, PackShipQO qo) {
        assertPlatform();
        if (qo.getStatus() != null && !qo.getStatus().isEmpty()) {
            qo.setDbStatuses(java.util.Collections.singletonList(qo.getStatus()));
        } else {
            qo.setDbStatuses(DEFAULT_SCOPE);
        }
        IPage<PackShipOrderVO> page = PageUtil.prodPage(pageParam);
        outboundShippingMapper.pageOrders(page, qo);
        List<PackShipOrderVO> records = page.getRecords();
        records.forEach(r -> {
            r.setStatus(OutboundPickingService.toViewStatus(r.getStatus()));
            r.setNeedPhoto(Boolean.FALSE);
        });
        return new PageResult<>(records, page.getTotal());
    }

    public PackShipOrderVO getDetail(Long id) {
        assertPlatform();
        PackShipOrderVO vo = outboundShippingMapper.selectOrderById(id);
        Assert.notNull(vo, "出库单不存在");
        vo.setStatus(OutboundPickingService.toViewStatus(vo.getStatus()));
        vo.setItems(buildItems(id));
        vo.setNeedPhoto(computeNeedPhoto(id));
        return vo;
    }

    public List<LogisticsChannelVO> listChannels() {
        assertPlatform();
        List<LogisticsChannelVO> channels = new ArrayList<>();
        LogisticsChannelVO auto = new LogisticsChannelVO();
        auto.setCode("AUTO");
        auto.setName("自动选择渠道");
        channels.add(auto);
        channels.addAll(outboundShippingMapper.selectChannels());
        return channels;
    }

    // ==================== 打包 ====================

    @Transactional(rollbackFor = Exception.class)
    public void pack(PackDTO dto) {
        assertPlatform();
        SalesOutboundOrder order = salesOutboundMapper.selectById(dto.getOutboundOrderId());
        Assert.notNull(order, "出库单不存在");
        if (!OutboundOrderStatus.PICKING.name().equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅拣货中的出库单可以打包");
        }
        order.setOrderStatus(OutboundOrderStatus.PACKED.name());
        order.setPackMode(dto.getPackMode());
        order.setPackerName(dto.getPackerName());
        salesOutboundMapper.updateById(order);
        log.info("打包完成, outboundId={}, packMode={}", order.getId(), dto.getPackMode());
    }

    // ==================== 签出（扣库存 + 释放锁定 + 计费） ====================

    @Transactional(rollbackFor = Exception.class)
    public ShipResultVO ship(ShipDTO dto) {
        assertPlatform();
        SalesOutboundOrder order = salesOutboundMapper.selectById(dto.getOutboundOrderId());
        Assert.notNull(order, "出库单不存在");
        String st = order.getOrderStatus();
        if (OutboundOrderStatus.SHIPPED.name().equals(st) || OutboundOrderStatus.COMPLETED.name().equals(st)) {
            throw new BusinessException(400, "该出库单已签出，请勿重复操作");
        }
        if (!OutboundOrderStatus.PACKED.name().equals(st)) {
            throw new BusinessException(400, "仅已打包的出库单可以签出");
        }
        // BR-04：次品/电子类强制拍照
        if (computeNeedPhoto(order.getId()) && (dto.getPhotoCount() == null || dto.getPhotoCount() <= 0)) {
            throw new BusinessException(400, "次品/电子类出库签出必须上传照片");
        }

        // 状态 CAS 抢占：PACKED→SHIPPED 原子推进，并发/重试只有一个赢家，消灭 TOCTOU
        // （SalesOutboundOrder 无 @Version，靠 WHERE order_status='PACKED' 条件更新保证幂等）。
        int claimed = salesOutboundMapper.casOrderStatus(order.getId(),
                OutboundOrderStatus.PACKED.name(), OutboundOrderStatus.SHIPPED.name());
        if (claimed != 1) {
            throw new BusinessException(400, "该出库单已签出或正在处理，请勿重复操作");
        }

        // 扣物理库存 + 释放锁定（据下架 FIFO 分配）
        List<WmsOutboundPickAllocation> allocs = pickAllocationMapper.selectByOutboundOrderId(order.getId());
        Assert.notEmpty(allocs, "该出库单无拣货分配记录，无法签出");
        Set<String> touchedSku = new LinkedHashSet<>();
        Set<Long> touchedPallets = new LinkedHashSet<>();
        for (WmsOutboundPickAllocation a : allocs) {
            WmsPhysicalInventory batch = physicalInventoryMapper.selectById(a.getPhysicalInventoryId());
            Assert.notNull(batch, "批次不存在: " + a.getPhysicalInventoryId());
            int take = a.getTakeQty() == null ? 0 : a.getTakeQty();
            int newQty = (batch.getQuantity() == null ? 0 : batch.getQuantity()) - take;
            int newReserved = (batch.getReservedQty() == null ? 0 : batch.getReservedQty()) - take;
            Assert.isTrue(newQty >= 0, "批次数量不足，签出失败: " + batch.getId());
            batch.setQuantity(newQty);
            batch.setReservedQty(Math.max(newReserved, 0));
            // @Version 乐观锁：updateById 带 AND version=?，返回 0 表示批次被并发改动，
            // 必须抛异常回滚（否则扣减被静默丢弃 → 幻量库存）。
            int updated = physicalInventoryMapper.updateById(batch);
            if (updated != 1) {
                throw new BusinessException(409, "批次库存版本冲突，签出失败，请重试: " + batch.getId());
            }
            touchedSku.add(a.getSkuCode());
            if (batch.getPalletId() != null) {
                touchedPallets.add(batch.getPalletId());
            }
        }
        // 同事务聚合刷新受影响 SKU 的仓库级快照
        for (String sku : touchedSku) {
            inventoryAggregator.refreshSnapshot(0L, order.getErpTenantId(), order.getWarehouseId(), sku);
        }
        for (Long palletId : touchedPallets) {
            palletService.refreshAfterOutbound(palletId);
        }

        // 链路二物流费：出库单关联物流产品(货主建单时选) → 按产品统一单价每次使用计一笔（幂等 biz_id），
        // 归属 wms_tenant_id = 货主的父服务商（服务商收入页据此聚合）。
        BigDecimal fee = resolveShippingFee(order);
        Long billingRecordId = null;
        if (order.getLogisticsProductId() != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            String bizId = "SHIP:" + order.getId();
            WmsClientBillingRecord exist = clientBillingRecordMapper.selectByBizId(bizId);
            if (exist != null) {
                billingRecordId = exist.getId();
                fee = exist.getAmount();
            } else {
                WmsClientBillingRecord rec = new WmsClientBillingRecord();
                rec.setBizId(bizId);
                rec.setWmsTenantId(resolveParentOperatorId(order.getErpTenantId()));
                rec.setErpTenantId(order.getErpTenantId());
                rec.setOutboundOrderId(order.getId());
                rec.setFeeType("SHIPPING");
                rec.setAmount(fee);
                rec.setCurrency("RUB");
                rec.setTrackingNo(dto.getTrackingNo());
                rec.setLogisticsProductId(order.getLogisticsProductId());
                rec.setBillMonth(order.getOutboundDate() == null ? null
                        : order.getOutboundDate().toString().substring(0, 7));
                clientBillingRecordMapper.insert(rec);
                billingRecordId = rec.getId();
            }
        }

        String channelName = resolveChannelName(dto.getChannel());
        order.setOrderStatus(OutboundOrderStatus.SHIPPED.name());
        order.setChannelName(channelName);
        order.setTrackingNo(dto.getTrackingNo());
        order.setWeight(dto.getWeight());
        order.setShippingFee(fee);
        salesOutboundMapper.updateById(order);
        log.info("签出完成, outboundId={}, tracking={}, fee={}", order.getId(), dto.getTrackingNo(), fee);

        ShipResultVO vo = new ShipResultVO();
        vo.setTrackingNo(dto.getTrackingNo());
        vo.setChannelName(channelName);
        vo.setShippingFee(fee);
        vo.setBillingRecordId(billingRecordId);
        return vo;
    }

    // ==================== 组装 / 辅助 ====================

    private List<PackShipItemVO> buildItems(Long orderId) {
        List<SalesOutboundOrderItem> items = salesOutboundItemMapper.selectByOutboundOrderId(orderId);
        List<PackShipItemVO> result = new ArrayList<>();
        for (SalesOutboundOrderItem i : items) {
            PackShipItemVO vo = new PackShipItemVO();
            vo.setSkuCode(i.getSkuCode());
            vo.setQty(i.getQuantity());
            // 下架 FIFO 仅取良品，故出库明细品质为良品；次品出库属特例（v2 支持）
            vo.setQuality("GOOD");
            result.add(vo);
        }
        return result;
    }

    /**
     * 是否需要签出拍照（BR-04：次品或电子类）。当前下架仅取良品、且暂无商品电子类目主数据，返回 false；
     * 保留判定入口，接入品类主数据后可扩展。
     */
    private boolean computeNeedPhoto(Long orderId) {
        return false;
    }

    /** 链路二物流费定价：出库单关联物流产品的统一单价（每次使用计一次）；无产品/已停用 → 0 不计费。 */
    private BigDecimal resolveShippingFee(SalesOutboundOrder order) {
        com.erp.admin.wms.model.entity.WmsLogisticsProduct product = logisticsProductService
            .getEnabledById(order.getLogisticsProductId());
        return (product == null || product.getUnitPrice() == null) ? BigDecimal.ZERO : product.getUnitPrice();
    }

    /** 货主的父服务商（计费归属）；无父级兜底 0（不归任何服务商，收入页不可见）。 */
    private Long resolveParentOperatorId(Long erpTenantId) {
        if (erpTenantId == null) {
            return 0L;
        }
        com.erp.admin.tenant.model.entity.SysTenant tenant = sysTenantMapper.selectById(erpTenantId);
        return (tenant == null || tenant.getParentWmsTenantId() == null) ? 0L : tenant.getParentWmsTenantId();
    }

    private String resolveChannelName(String channelCode) {
        if (channelCode == null || "AUTO".equals(channelCode)) {
            return "自动选择渠道";
        }
        for (LogisticsChannelVO c : outboundShippingMapper.selectChannels()) {
            if (channelCode.equals(c.getCode())) {
                return c.getName();
            }
        }
        return channelCode;
    }

    private void assertPlatform() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可执行出库作业");
        }
    }

}
