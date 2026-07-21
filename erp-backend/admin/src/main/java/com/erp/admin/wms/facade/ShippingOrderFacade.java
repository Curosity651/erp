package com.erp.admin.wms.facade;

import com.erp.admin.wms.model.enums.SourceType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.model.dto.ShippingOrderDTO;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.entity.ShippingOrder;
import com.erp.admin.wms.model.entity.ShippingOrderAdjust;
import com.erp.admin.wms.model.entity.ShippingOrderItem;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.enums.ShippingStatus;
import com.erp.admin.wms.model.result.StockPostingResult;
import com.erp.admin.wms.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物流单业务编排层
 * <p>
 * 负责跨服务的业务流程编排，持有事务。
 * Service 层专注于单一领域的 CRUD 和状态管理。
 * </p>
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingOrderFacade {

    private final ShippingOrderService shippingOrderService;
    private final ShippingOrderItemService shippingOrderItemService;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderItemService purchaseOrderItemService;
    private final StockPostingService stockPostingService;
    private final ShippingOrderAdjustService shippingOrderAdjustService;
    private final RegionService regionService;
    private final PurchaseInboundService purchaseInboundService;

    /**
     * 创建物流单
     *
     * @param dto 物流单DTO
     * @return 物流单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ShippingOrderDTO dto) {
        Long orderId = shippingOrderService.createOrder(dto);
        List<ShippingOrderItem> items = shippingOrderItemService.getByShippingOrderId(orderId);
        shippingOrderItemService.validateAndLockShippingAllocation(orderId, items);
        log.info("Created shipping order via facade, id={}", orderId);
        return orderId;
    }

    /**
     * 删除物流单
     *
     * @param ids 物流单ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            ShippingOrder order = shippingOrderService.getByIdOrThrow(id);
            ShippingStatus status = ShippingStatus.valueOf(order.getShippingStatus());
            Assert.isTrue(status == ShippingStatus.PENDING, "只有待发货状态的物流单可以删除");
            Assert.isTrue(!Integer.valueOf(1).equals(order.getPaymentStatus()), "已付款的物流单不允许删除");
            shippingOrderService.deleteOrder(id);
            log.info("Deleted shipping order via facade, id={}", id);
        }
    }

    /**
     * 确认发货
     * 1. 采购单明细：增加已发货数量（乐观校验）
     * 2. 更新采购单发货状态
     * 3. 库存过账（在途+）
     * 4. 更新物流单状态
     *
     * @param id 物流单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmShip(Long id) {
        ShippingOrder order = shippingOrderService.getByIdOrThrow(id);

        ShippingStatus currentStatus = ShippingStatus.valueOf(order.getShippingStatus());
        Assert.isTrue(currentStatus == ShippingStatus.PENDING, "只有待发货状态的物流单可以确认发货");
        Assert.notNull(order.getTargetRegionId(), "目标区域不能为空");

        List<ShippingOrderItem> items = shippingOrderItemService.getByShippingOrderId(id);
        Assert.notEmpty(items, "物流单明细不能为空");

        // 1. 锁定采购明细并扣除其他待发货草稿的占用
        shippingOrderItemService.validateAndLockShippingAllocation(id, items);

        // 2. 采购单明细：增加已发货数量（乐观校验）
        purchaseOrderItemService.increaseShippedQuantity(items);

        // 3. 更新采购单发货状态
        updatePurchaseOrderShippingStatus(items);

        // 4. 库存过账
        StockPostingResult result = doStockPosting(order, items);

        // 5. 更新物流单状态
        shippingOrderService.updateToShipped(id, result.getPostingId());

        log.info("Confirmed shipping via facade, id={}, postingId={}", id, result.getPostingId());
    }

    /**
     * 编辑物流单
     * 根据状态分发到不同的处理逻辑
     *
     * @param dto 物流单DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(ShippingOrderDTO dto) {
        ShippingOrder order = shippingOrderService.getByIdOrThrow(dto.getId());

        ShippingStatus status = ShippingStatus.valueOf(order.getShippingStatus());
        Assert.isTrue(status != ShippingStatus.COMPLETED, "已完成的物流单不允许编辑");

        if (status == ShippingStatus.PENDING) {
            updateForPending(order, dto);
        } else if (status == ShippingStatus.SHIPPED) {
            updateForShipped(order, dto);
        } else {
            // PARTIAL_ARRIVED / ALL_ARRIVED
            updateForArrived(order, dto);
        }

        log.info("Updated shipping order via facade, id={}, status={}", dto.getId(), status);
    }

    /**
     * PENDING 状态更新：全部字段可编辑
     */
    private void updateForPending(ShippingOrder order, ShippingOrderDTO dto) {
        // 1. 校验明细不为空
        Assert.notEmpty(dto.getItems(), "货物明细不能为空");

        // 2. 校验费用字段
        validateCostFields(dto);

        // 3. 更新物流单主体 + 明细
        shippingOrderService.updateWithItems(order.getId(), dto);
    }

    /**
     * SHIPPED 状态更新：目标区域、备注、付款信息可编辑；未付款时物流/费用可编辑
     */
    private void updateForShipped(ShippingOrder order, ShippingOrderDTO dto) {
        Long oldRegionId = order.getTargetRegionId();
        Long newRegionId = dto.getTargetRegionId();

        // 物流/费用变更：已付款时禁止修改
        if (hasLogisticsOrCostChange(order, dto)) {
            Assert.isTrue(order.getPaymentStatus() == null || order.getPaymentStatus() != 1,
                "已付款的物流单不允许修改物流和费用信息");
            validateCostFields(dto);
        }

        // 区域变更处理
        if (!Objects.equals(oldRegionId, newRegionId)) {
            // 校验是否存在已确认的入库单
            boolean hasConfirmedInbound = purchaseInboundService
                .hasConfirmedInboundByShippingOrderId(order.getId());
            Assert.isTrue(!hasConfirmedInbound,
                "该物流单已有确认入库的记录，不允许修改目标区域");

            doRegionRedirect(order, oldRegionId, newRegionId);
        }

        // 更新允许修改的字段
        shippingOrderService.updateForShipped(order.getId(), dto,
            !Objects.equals(oldRegionId, newRegionId) ? newRegionId : null);
    }

    /**
     * ARRIVED 状态更新：备注、付款信息可编辑；未付款时物流/费用可编辑
     */
    private void updateForArrived(ShippingOrder order, ShippingOrderDTO dto) {
        // 物流/费用变更：已付款时禁止修改
        if (hasLogisticsOrCostChange(order, dto)) {
            Assert.isTrue(order.getPaymentStatus() == null || order.getPaymentStatus() != 1,
                "已付款的物流单不允许修改物流和费用信息");
            validateCostFields(dto);
        }

        shippingOrderService.updateForArrived(order.getId(), dto);
    }

    /**
     * 执行区域调整（LOGISTICS_REDIRECT）
     */
    private void doRegionRedirect(ShippingOrder order, Long oldRegionId, Long newRegionId) {
        Region oldRegion = regionService.getById(oldRegionId);
        Region newRegion = regionService.getById(newRegionId);
        String remark = String.format("区域调整：%s → %s",
            oldRegion != null ? oldRegion.getRegionName() : oldRegionId,
            newRegion != null ? newRegion.getRegionName() : newRegionId);

        // 1. 创建调整记录
        ShippingOrderAdjust adjust = shippingOrderAdjustService.createAdjust(
            order.getId(), oldRegionId, newRegionId, remark);

        // 2. 获取物流单明细
        List<ShippingOrderItem> items = shippingOrderItemService.getByShippingOrderId(order.getId());
        Assert.notEmpty(items, "物流单明细不能为空");

        // 3. 构建跨区域双 Item
        List<StockPostingItemDTO> postingItems = new ArrayList<>();
        for (ShippingOrderItem item : items) {
            // 原目标区域 IN_TRANSIT OUT
            postingItems.add(StockPostingItemDTO.builder()
                .regionId(oldRegionId)
                .erpTenantId(order.getErpTenantId())
                .skuCode(item.getSkuCode())
                .bucket(StockBucket.IN_TRANSIT)
                .direction(StockDirection.OUT)
                .quantity(item.getQuantity())
                .build());
            // 新目标区域 IN_TRANSIT IN
            postingItems.add(StockPostingItemDTO.builder()
                .regionId(newRegionId)
                .erpTenantId(order.getErpTenantId())
                .skuCode(item.getSkuCode())
                .bucket(StockBucket.IN_TRANSIT)
                .direction(StockDirection.IN)
                .quantity(item.getQuantity())
                .build());
        }

        // 4. 库存过账
        StockPostingDTO postingDTO = StockPostingDTO.builder()
            .postingType(PostingType.LOGISTICS_REDIRECT)
            .sourceType(SourceType.SHIPPING_ORDER.name())
            .sourceId(adjust.getId())
            .sourceNo(adjust.getAdjustNo())
            .regionId(newRegionId)
            .erpTenantId(order.getErpTenantId())
            .remark(remark)
            .items(postingItems)
            .build();

        StockPostingResult result = stockPostingService.post(postingDTO);

        // 5. 更新调整记录的过账ID
        shippingOrderAdjustService.updateStockPostingId(adjust.getId(), result.getPostingId());

        log.info("Region redirect completed, orderId={}, adjustId={}, postingId={}, {} -> {}",
            order.getId(), adjust.getId(), result.getPostingId(), oldRegionId, newRegionId);
    }

    /**
     * 检查物流/费用字段是否有变更
     */
    private boolean hasLogisticsOrCostChange(ShippingOrder order, ShippingOrderDTO dto) {
        return !Objects.equals(order.getShippingMethod(), dto.getShippingMethod())
            || !Objects.equals(order.getShippingRoute(), dto.getShippingRoute())
            || !Objects.equals(order.getTotalWeight(), dto.getTotalWeight())
            || !Objects.equals(order.getUnitPrice(), dto.getUnitPrice())
            || !Objects.equals(order.getShippingFee(), dto.getShippingFee())
            || !Objects.equals(order.getMiscFee(), dto.getMiscFee())
            || !Objects.equals(order.getTotalAmount(), dto.getTotalAmount())
            || !Objects.equals(order.getTotalAmountCny(), dto.getTotalAmountCny());
    }

    /**
     * 校验费用字段
     */
    private void validateCostFields(ShippingOrderDTO dto) {
        String shippingMethod = dto.getShippingMethod();

        if ("GRAY".equals(shippingMethod)) {
            Assert.notNull(dto.getUnitPrice(), "灰关模式下，物流单价不能为空");
            Assert.isTrue(dto.getUnitPrice().compareTo(BigDecimal.ZERO) > 0,
                "灰关模式下，物流单价必须大于0");
        } else if ("WHITE".equals(shippingMethod)) {
            Assert.notNull(dto.getShippingFee(), "白关模式下，运输费用不能为空");
            Assert.isTrue(dto.getShippingFee().compareTo(BigDecimal.ZERO) > 0,
                "白关模式下，运输费用必须大于0");
            Assert.notNull(dto.getMiscFee(), "白关模式下，杂费不能为空");
            Assert.isTrue(dto.getMiscFee().compareTo(BigDecimal.ZERO) >= 0,
                "白关模式下，杂费不能为负数");
        } else {
            throw new IllegalArgumentException("无效的物流方式: " + shippingMethod);
        }
    }

    /**
     * 执行库存过账
     */
    private StockPostingResult doStockPosting(ShippingOrder order, List<ShippingOrderItem> items) {
        List<StockPostingItemDTO> postingItems = items.stream()
                .map(item -> StockPostingItemDTO.builder()
                        .regionId(order.getTargetRegionId())
                        .erpTenantId(order.getErpTenantId())
                        .skuCode(item.getSkuCode())
                        .bucket(StockBucket.IN_TRANSIT)
                        .direction(StockDirection.IN)
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        StockPostingDTO postingDTO = StockPostingDTO.builder()
                .postingType(PostingType.LOGISTICS_SHIP)
                .sourceType(SourceType.SHIPPING_ORDER.name())
                .sourceId(order.getId())
                .sourceNo(order.getShippingNo())
                .regionId(order.getTargetRegionId())
                .erpTenantId(order.getErpTenantId())
                .items(postingItems)
                .build();

        return stockPostingService.post(postingDTO);
    }

    /**
     * 更新采购单发货状态
     */
    private void updatePurchaseOrderShippingStatus(List<ShippingOrderItem> items) {
        Set<Long> purchaseOrderIds = items.stream()
                .map(ShippingOrderItem::getPurchaseOrderId)
                .collect(Collectors.toSet());

        for (Long purchaseOrderId : purchaseOrderIds) {
            purchaseOrderService.updateShippingStatus(purchaseOrderId);
        }
    }

}
