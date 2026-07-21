package com.erp.admin.wms.facade;

import com.erp.admin.wms.model.dto.CustomReturnDTO;
import com.erp.admin.wms.model.dto.ManualInboundDTO;
import com.erp.admin.wms.model.dto.PurchaseInboundDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.ShippingOrder;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.PurchaseInboundStatus;
import com.erp.admin.wms.model.enums.ShippingStatus;
import com.erp.admin.wms.model.enums.WarehouseTypeEnum;
import com.erp.admin.wms.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * 采购/自定义入库单业务编排层
 * <p>
 * 负责跨服务的业务流程编排，持有事务。Service 层专注于单一领域的 CRUD 和状态管理。
 * </p>
 *
 * <p>D2 重构（去中转层）：货主在此层建单并「提交」(DRAFT→SUBMITTED)，单据随后流转给平台超管
 * 收货/上架（{@link com.erp.admin.wms.service.WmsInboundExecutionService}），状态回到同一张单。
 * 旧的一键「确认并通过」(confirm/createAndConfirm) 已退役。</p>
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseInboundFacade {

    private final PurchaseInboundService purchaseInboundService;
    private final PurchaseInboundItemService purchaseInboundItemService;
    private final ShippingOrderItemService shippingOrderItemService;
    private final ShippingOrderService shippingOrderService;
    private final WarehouseService warehouseService;

    /**
     * 校验仓库属于指定区域内的 OWN 仓库
     * @param warehouseId 仓库ID
     * @param regionId 区域ID
     */
    private void validateWarehouseInRegion(Long warehouseId, Long regionId) {
        Warehouse warehouse = warehouseService.getById(warehouseId);
        Assert.notNull(warehouse, "仓库不存在");
        Assert.isTrue(WarehouseTypeEnum.OWN.getCode().equals(warehouse.getWarehouseType()),
            "入库仓库必须是自有仓(OWN)类型");
        Assert.isTrue(Objects.equals(warehouse.getRegionId(), regionId),
            "入库仓库必须在物流单目标区域内");
    }

    /**
     * 校验仓库是自有仓(OWN)
     * @param warehouseId 仓库ID
     */
    private void validateWarehouseIsOwn(Long warehouseId) {
        Warehouse warehouse = warehouseService.getById(warehouseId);
        Assert.notNull(warehouse, "仓库不存在");
        Assert.isTrue(WarehouseTypeEnum.OWN.getCode().equals(warehouse.getWarehouseType()),
            "入库仓库必须是自有仓(OWN)类型");
    }

    /**
     * 创建采购入库单
     * 1. 校验物流单状态
     * 2. 校验仓库
     * 3. 创建入库单和明细
     *
     * @param dto 入库单DTO
     * @return 入库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseInboundDTO dto) {
        // 1. 校验物流单
        ShippingOrder shippingOrder = shippingOrderService.getByIdOrThrow(dto.getShippingOrderId());
        Assert.isTrue(!ShippingStatus.PENDING.name().equals(shippingOrder.getShippingStatus()),
                "物流单尚未发货，不能创建入库单");

        // 2. 校验仓库属于物流单目标区域内的 OWN 仓库
        Assert.notNull(dto.getWarehouseId(), "入库仓库不能为空");
        validateWarehouseInRegion(dto.getWarehouseId(), shippingOrder.getTargetRegionId());

        // 3. 创建入库单
        Long orderId = purchaseInboundService.createOrder(dto);

        log.info("Created purchase inbound order via facade, id={}", orderId);
        return orderId;
    }

    /**
     * 创建自定义入库单（不经采购链，仅校验仓库为自有仓）
     *
     * @param dto 自定义入库单DTO
     * @return 入库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(ManualInboundDTO dto) {
        Assert.notNull(dto.getWarehouseId(), "入库仓库不能为空");
        validateWarehouseIsOwn(dto.getWarehouseId());

        Long orderId = purchaseInboundService.createManualOrder(dto);
        log.info("Created manual inbound order via facade, id={}", orderId);
        return orderId;
    }

    /**
     * 创建自定义退货单（不挂靠平台订单，仅校验仓库为自有仓）
     *
     * @param dto 自定义退货单DTO
     * @return 退货单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomReturn(CustomReturnDTO dto) {
        Assert.notNull(dto.getWarehouseId(), "入库仓库不能为空");
        validateWarehouseIsOwn(dto.getWarehouseId());

        Long orderId = purchaseInboundService.createCustomReturnOrder(dto);
        log.info("Created custom return order via facade, id={}", orderId);
        return orderId;
    }

    /**
     * 提交入库单（草稿 → 已提交），提交后流转给平台收货/上架。
     *
     * @param id 入库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        PurchaseInboundOrder order = purchaseInboundService.getByIdOrThrow(id);
        PurchaseInboundStatus status = PurchaseInboundStatus.valueOf(order.getOrderStatus());
        Assert.isTrue(status == PurchaseInboundStatus.DRAFT, "只有草稿状态的入库单可以提交");

        List<PurchaseInboundOrderItem> items = purchaseInboundItemService.getByInboundOrderId(id);
        Assert.notEmpty(items, "入库明细不能为空");

        if (order.getShippingOrderId() != null) {
            shippingOrderItemService.reserveInboundQuantities(order.getShippingOrderId(), items);
        }

        purchaseInboundService.updateToSubmitted(id);
        log.info("Submitted inbound order via facade, id={}", id);
    }

    /**
     * 取消入库单
     *
     * @param id 入库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        PurchaseInboundOrder order = purchaseInboundService.getByIdOrThrow(id);
        PurchaseInboundStatus status = PurchaseInboundStatus.valueOf(order.getOrderStatus());
        Assert.isTrue(status == PurchaseInboundStatus.DRAFT, "只有草稿状态的入库单可以取消");
        purchaseInboundService.updateToCancelled(id);
        log.info("Cancelled inbound order via facade, id={}", id);
    }

    /**
     * 删除入库单
     *
     * @param ids 入库单ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PurchaseInboundOrder order = purchaseInboundService.getByIdOrThrow(id);
            PurchaseInboundStatus status = PurchaseInboundStatus.valueOf(order.getOrderStatus());
            Assert.isTrue(status == PurchaseInboundStatus.DRAFT || status == PurchaseInboundStatus.CANCELLED,
                    "只有草稿或已取消状态的入库单可以删除");
            purchaseInboundService.deleteOrder(id);
            log.info("Deleted inbound order via facade, id={}", id);
        }
    }

}
