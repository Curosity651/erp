package com.erp.admin.wms.facade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.model.dto.PutawayDTO;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.TransferOrder;
import com.erp.admin.wms.model.entity.TransferOrderItem;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.enums.SourceType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.model.enums.TransferOrderStatus;
import com.erp.admin.wms.service.StockPostingService;
import com.erp.admin.wms.service.TransferOrderItemService;
import com.erp.admin.wms.service.TransferOrderService;
import com.erp.admin.wms.service.WmsLocationService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.service.WmsRackAssignmentService;
import com.erp.admin.wms.service.WmsZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 调拨单编排层（跨仓库·批次/库位级·两段式在途）。
 *
 * <p>把 A 仓某批次的货调到 B 仓、落在【该货主的服务商在 B 当前有效租用的库位】上；服务商在 B 无租库位则不能调。
 * 可用量一律由物理批次派生（发出扣 A 源批次 → A 可用降；到货在 B 目标库位建批次 → B 可用升）；在途桶由过账维护
 * （发出 B 在途+；到货/撤回 B 在途-），二者不重叠不双写。仅平台操作（权限在 Controller）。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferOrderFacade {

    private final TransferOrderService transferOrderService;
    private final TransferOrderItemService transferOrderItemService;
    private final StockPostingService stockPostingService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final WmsLocationService wmsLocationService;
    private final WmsZoneService wmsZoneService;
    private final WmsRackAssignmentService wmsRackAssignmentService;
    private final SysTenantMapper sysTenantMapper;

    /**
     * 确认发出（草稿→在途）：逐条扣源仓源批次(物理) + 过账 B 仓在途+。校验源批次可用、目标库位在该货主服务商 B 仓租架。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmShipment(Long id) {
        TransferOrder order = transferOrderService.getById(id);
        Assert.notNull(order, "调拨单不存在");
        Assert.isTrue(TransferOrderStatus.DRAFT.getCode().equals(order.getOrderStatus()),
                "只有草稿状态的调拨单可以确认发出");

        List<TransferOrderItem> items = transferOrderItemService.getByTransferOrderId(id);
        Assert.notEmpty(items, "调拨单明细不能为空");

        List<StockPostingItemDTO> inTransitIn = new ArrayList<>();
        for (TransferOrderItem item : items) {
            WmsPhysicalInventory source = physicalInventoryService.getById(item.getSourcePhysicalInventoryId());
            Assert.notNull(source, "源批次不存在：" + item.getSourcePhysicalInventoryId());
            Assert.isTrue(order.getFromWarehouseId().equals(source.getWarehouseId()), "源批次不属于源仓库");
            Assert.isTrue(item.getErpTenantId().equals(source.getErpTenantId()), "源批次不属于该货主");
            // 目标库位必须落在该货主服务商在 B 仓当前有效租用的排上
            resolveTargetZoneOrThrow(order.getToWarehouseId(), item.getErpTenantId(), item.getTargetLocationCode());

            // 扣源仓源批次（可用降）
            physicalInventoryService.deductForTransferShip(source.getId(), item.getQuantity(), order.getTransferNo());
            // B 仓在途 +
            inTransitIn.add(StockPostingItemDTO.builder()
                    .warehouseId(order.getToWarehouseId())
                    .erpTenantId(item.getErpTenantId())
                    .skuCode(item.getSkuCode())
                    .bucket(StockBucket.IN_TRANSIT)
                    .direction(StockDirection.IN)
                    .quantity(item.getQuantity())
                    .build());
        }
        postInTransit(order, inTransitIn, PostingType.TRANSFER_SHIP, order.getToWarehouseId());

        order.setOrderStatus(TransferOrderStatus.IN_TRANSIT.getCode());
        order.setShipTime(LocalDateTime.now());
        transferOrderService.updateById(order);
        log.info("Transfer shipped(batch/location), id={}, no={}", order.getId(), order.getTransferNo());
    }

    /**
     * 确认到货（在途→已入库）：逐条在 B 目标库位建批次(物理，B 可用升) + 过账 B 仓在途-。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long id) {
        TransferOrder order = transferOrderService.getById(id);
        Assert.notNull(order, "调拨单不存在");
        Assert.isTrue(TransferOrderStatus.IN_TRANSIT.getCode().equals(order.getOrderStatus()),
                "只有在途状态的调拨单可以确认到货");

        List<TransferOrderItem> items = transferOrderItemService.getByTransferOrderId(id);
        Assert.notEmpty(items, "调拨单明细不能为空");

        List<StockPostingItemDTO> inTransitOut = new ArrayList<>();
        for (TransferOrderItem item : items) {
            Long targetZoneId = resolveTargetZoneOrThrow(order.getToWarehouseId(), item.getErpTenantId(),
                    item.getTargetLocationCode());
            Long operatorId = ownerOperatorId(item.getErpTenantId());
            // 在 B 目标库位建批次（B 可用升）
            PutawayDTO put = new PutawayDTO();
            put.setWmsTenantId(operatorId == null ? 0L : operatorId);
            put.setErpTenantId(item.getErpTenantId());
            put.setWarehouseId(order.getToWarehouseId());
            put.setSkuCode(item.getSkuCode());
            put.setInboundItemId(0L);
            put.setInboundDate(LocalDate.now(ZoneOffset.UTC));
            put.setQuantity(item.getQuantity());
            put.setQuality("GOOD");
            put.setLocationCode(item.getTargetLocationCode());
            put.setZoneId(targetZoneId);
            put.setAllocatable(1);
            physicalInventoryService.putaway(put);
            // B 仓在途 -
            inTransitOut.add(StockPostingItemDTO.builder()
                    .warehouseId(order.getToWarehouseId())
                    .erpTenantId(item.getErpTenantId())
                    .skuCode(item.getSkuCode())
                    .bucket(StockBucket.IN_TRANSIT)
                    .direction(StockDirection.OUT)
                    .quantity(item.getQuantity())
                    .build());
            item.setReceivedQuantity(item.getQuantity());
            transferOrderItemService.updateById(item);
        }
        postInTransit(order, inTransitOut, PostingType.TRANSFER_RECEIVE, order.getToWarehouseId());

        order.setOrderStatus(TransferOrderStatus.COMPLETED.getCode());
        order.setEndTime(LocalDateTime.now());
        transferOrderService.updateById(order);
        log.info("Transfer received(batch/location), id={}, no={}", order.getId(), order.getTransferNo());
    }

    /**
     * 撤回（在途→已撤回）：把货加回源仓源批次(A 可用恢复) + 过账 B 仓在途-。
     */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        TransferOrder order = transferOrderService.getById(id);
        Assert.notNull(order, "调拨单不存在");
        Assert.isTrue(TransferOrderStatus.IN_TRANSIT.getCode().equals(order.getOrderStatus()),
                "只有在途状态的调拨单可以撤回");

        List<TransferOrderItem> items = transferOrderItemService.getByTransferOrderId(id);
        List<StockPostingItemDTO> inTransitOut = new ArrayList<>();
        for (TransferOrderItem item : items) {
            WmsPhysicalInventory source = physicalInventoryService.getById(item.getSourcePhysicalInventoryId());
            Assert.notNull(source, "源批次不存在：" + item.getSourcePhysicalInventoryId());
            // 加回源仓源批次原库位（A 可用恢复）：对原批次行加量，避免 putaway 盲插撞 uk_batch 唯一键
            physicalInventoryService.restoreForTransferRevoke(source.getId(), item.getQuantity(),
                    order.getTransferNo());
            inTransitOut.add(StockPostingItemDTO.builder()
                    .warehouseId(order.getToWarehouseId())
                    .erpTenantId(item.getErpTenantId())
                    .skuCode(item.getSkuCode())
                    .bucket(StockBucket.IN_TRANSIT)
                    .direction(StockDirection.OUT)
                    .quantity(item.getQuantity())
                    .build());
        }
        postInTransit(order, inTransitOut, PostingType.TRANSFER_CANCEL, order.getToWarehouseId());

        order.setOrderStatus(TransferOrderStatus.REVOKED.getCode());
        order.setEndTime(LocalDateTime.now());
        transferOrderService.updateById(order);
        log.info("Transfer revoked(batch/location), id={}, no={}", order.getId(), order.getTransferNo());
    }

    /**
     * 取消（草稿）：无库存影响。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TransferOrder order = transferOrderService.getById(id);
        Assert.notNull(order, "调拨单不存在");
        Assert.isTrue(TransferOrderStatus.DRAFT.getCode().equals(order.getOrderStatus()),
                "只有草稿状态的调拨单可以取消");
        order.setOrderStatus(TransferOrderStatus.CANCELLED.getCode());
        order.setEndTime(LocalDateTime.now());
        transferOrderService.updateById(order);
        log.info("Transfer cancelled, id={}, no={}", order.getId(), order.getTransferNo());
    }

    // ==================== 内部 ====================

    private void postInTransit(TransferOrder order, List<StockPostingItemDTO> items, PostingType type,
            Long postingWarehouseId) {
        if (items.isEmpty()) {
            return;
        }
        stockPostingService.post(StockPostingDTO.builder()
                .warehouseId(postingWarehouseId)
                .erpTenantId(items.get(0).getErpTenantId())
                .postingType(type)
                .sourceType(SourceType.TRANSFER.name())
                .sourceId(order.getId())
                .sourceNo(order.getTransferNo())
                .items(items)
                .build());
    }

    /** 校验目标库位落在该货主服务商 B 仓当前有效租用排上，返回目标库位分区ID；否则抛错。 */
    private Long resolveTargetZoneOrThrow(Long toWarehouseId, Long erpTenantId, String targetLocationCode) {
        Assert.hasText(targetLocationCode, "目标库位不能为空");
        WmsLocation target = wmsLocationService.listByWarehouse(toWarehouseId).stream()
                .filter(l -> targetLocationCode.equals(l.getLocationCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "目标仓库无此库位：" + targetLocationCode));
        Long operatorId = ownerOperatorId(erpTenantId);
        if (operatorId == null) {
            throw new BusinessException(400, "该货主未绑定服务商，无法调拨");
        }
        Set<String> racks = wmsRackAssignmentService.activeRackNos(toWarehouseId, operatorId);
        if (!racks.contains(target.getRackNo())) {
            throw new BusinessException(400,
                    "目标库位不在该货主服务商于目标仓的租用范围（服务商在该仓无库位则不能调拨）：" + targetLocationCode);
        }
        return target.getZoneId();
    }

    /** 货主 → 父服务商 wms_tenant_id。 */
    private Long ownerOperatorId(Long erpTenantId) {
        if (erpTenantId == null) {
            return null;
        }
        SysTenant owner = sysTenantMapper.selectById(erpTenantId);
        return owner == null ? null : owner.getParentWmsTenantId();
    }

}
