package com.erp.admin.wms.facade;

import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.wms.model.enums.SourceType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.model.dto.SalesOutboundDTO;
import com.erp.admin.wms.model.dto.SalesOutboundItemDTO;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.result.StockPostingResult;
import com.erp.admin.wms.model.vo.StockShortageVO;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.service.OutboundPickingService;
import com.erp.admin.wms.service.PlatformRegionMappingService;
import com.erp.admin.wms.service.RegionInventoryService;
import com.erp.admin.wms.service.SalesOutboundItemService;
import com.erp.admin.wms.service.SalesOutboundService;
import com.erp.admin.wms.service.StockPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售出库单业务编排层
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
public class SalesOutboundFacade {

    private final SalesOutboundService salesOutboundService;
    private final SalesOutboundItemService salesOutboundItemService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final ErpOrderService erpOrderService;
    private final StockPostingService stockPostingService;
    private final PlatformRegionMappingService platformRegionMappingService;
    private final RegionInventoryService regionInventoryService;
    private final OutboundPickingService outboundPickingService;

    /**
     * 创建出库单
     * 1. 校验订单状态和平台
     * 2. 创建出库单和明细
     * 3. 更新订单出库状态为 ALLOCATED（使用乐观锁）
     *
     * @param dto 出库单DTO
     * @return 出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalesOutboundDTO dto) {
        // 1. 校验订单（使用乐观锁）
        validateAndAllocateOrders(dto);

        // 2. 创建出库单
        Long orderId = salesOutboundService.saveOrder(dto);

        log.info("Created sales outbound order via facade, id={}, platform={}", orderId, dto.getPlatform());
        return orderId;
    }

    /**
     * 更新出库单
     * 1. 释放原订单占用
     * 2. 校验并占用新订单
     * 3. 更新出库单和明细
     *
     * @param dto 出库单DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SalesOutboundDTO dto) {
        SalesOutboundOrder order = salesOutboundService.getByIdOrThrow(dto.getId());
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                "只有草稿状态的出库单可以编辑");

        // 1. 获取原明细并释放订单占用
        List<SalesOutboundOrderItem> oldItems = salesOutboundItemService.getByOutboundOrderId(order.getId());
        List<Long> oldOrderIds = oldItems.stream()
                .map(SalesOutboundOrderItem::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (!oldOrderIds.isEmpty()) {
            erpOrderService.releaseOutboundAllocation(oldOrderIds);
        }

        // 2. 校验并占用新订单
        validateAndAllocateOrders(dto);

        // 3. 更新出库单
        salesOutboundService.updateOrder(dto);

        log.info("Updated sales outbound order via facade, id={}", dto.getId());
    }

    /**
     * 删除出库单
     * 1. 释放订单占用
     * 2. 删除出库单和明细
     *
     * @param ids 出库单ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SalesOutboundOrder order = salesOutboundService.getByIdOrThrow(id);
            Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                    "只有草稿状态的出库单可以删除");

            // 1. 释放订单占用
            List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
            List<Long> orderIds = items.stream()
                    .map(SalesOutboundOrderItem::getErpOrderId)
                    .distinct()
                    .collect(Collectors.toList());
            if (!orderIds.isEmpty()) {
                erpOrderService.releaseOutboundAllocation(orderIds);
            }

            // 2. 删除出库单
            salesOutboundService.deleteOrder(id);

            log.info("Deleted sales outbound order via facade, id={}", id);
        }
    }

    /**
     * 取消出库单
     * 1. 释放订单占用
     * 2. 更新出库单状态
     *
     * @param id 出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SalesOutboundOrder order = salesOutboundService.getByIdOrThrow(id);
        String status = order.getOrderStatus();
        // 方案A：允许 草稿(DRAFT) 或 已确认待下架(CONFIRMED) 取消；平台一旦下架(PICKING)及之后拒绝取消
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(status)
                        || OutboundOrderStatus.CONFIRMED.name().equals(status),
                "仅待下架前(草稿/已确认)的出库单可以取消");

        List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);

        // 1. 已确认单：释放批次预留（可用恢复）+ 区域预占加回（与确认的区域 RESERVED- 对称）
        if (OutboundOrderStatus.CONFIRMED.name().equals(status)) {
            outboundPickingService.releaseForOrder(order);
            reverseRegionReservation(order, items);
        }

        // 2. 释放 ERP 订单占用
        List<Long> orderIds = items.stream()
                .map(SalesOutboundOrderItem::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            erpOrderService.releaseOutboundAllocation(orderIds);
        }

        // 3. 更新出库单状态
        salesOutboundService.updateToCancelled(id);

        log.info("Cancelled sales outbound order via facade, id={}, fromStatus={}", id, status);
    }

    /**
     * 取消已确认出库时，把确认阶段释放的“区域预占”加回（区域 RESERVED IN），与 {@link #doStockPosting} 对称。
     * 用 SALES_RELEASE 作过账类型，与确认的 SALES_SHIP 幂等键区分，避免被去重跳过。
     */
    private void reverseRegionReservation(SalesOutboundOrder order, List<SalesOutboundOrderItem> items) {
        Long regionId = platformRegionMappingService.getRegionIdByPlatform(order.getPlatform());
        Assert.notNull(regionId, "平台 " + order.getPlatform() + " 未配置区域映射");

        Map<String, Integer> skuQuantityMap = items.stream()
                .collect(Collectors.groupingBy(
                        SalesOutboundOrderItem::getSkuCode,
                        Collectors.summingInt(SalesOutboundOrderItem::getQuantity)));

        List<StockPostingItemDTO> postingItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            postingItems.add(StockPostingItemDTO.builder()
                    .warehouseId(0L)
                    .regionId(regionId)
                    .erpTenantId(order.getErpTenantId())
                    .skuCode(entry.getKey())
                    .bucket(StockBucket.RESERVED)
                    .direction(StockDirection.IN)
                    .quantity(entry.getValue())
                    .build());
        }

        StockPostingDTO postingDTO = StockPostingDTO.builder()
                .postingType(PostingType.SALES_RELEASE)
                .sourceType(SourceType.SALES_OUTBOUND.name())
                .sourceId(order.getId())
                .sourceNo(order.getOutboundNo())
                .warehouseId(order.getWarehouseId())
                .regionId(regionId)
                .erpTenantId(order.getErpTenantId())
                .items(postingItems)
                .build();

        stockPostingService.post(postingDTO);
    }

    /**
     * 确认出库
     * 1. 校验订单状态（使用乐观锁）
     * 2. 校验库存是否充足
     * 3. 执行库存过账
     * 4. 更新订单出库状态为 COMPLETED
     * 5. 更新出库单状态
     *
     * @param id 出库单ID
     * @return 库存不足明细列表（空列表表示成功）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<StockShortageVO> confirm(Long id) {
        SalesOutboundOrder order = salesOutboundService.getByIdOrThrow(id);
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                "只有草稿状态的出库单可以确认");

        List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
        Assert.notEmpty(items, "出库明细不能为空");

        // 1. 库存充足性预检（快照，快速失败）
        List<StockShortageVO> shortages = checkStockSufficiency(order, items);
        if (!shortages.isEmpty()) {
            return shortages;
        }

        // 2. 批次预留（方案A）：确认时就 FIFO 锁批次 + reserved_qty += + 建拣货分配 + 刷新快照，
        //    可用数当场从批次算着下降。缺货则返回明细，事务回滚、单据保持 DRAFT。
        List<StockShortageVO> reserveShortages = outboundPickingService.reserveForOrder(order, items);
        if (!reserveShortages.isEmpty()) {
            return reserveShortages;
        }

        // 3. 校验并占用 ERP 订单（乐观锁）
        List<Long> orderIds = items.stream()
                .map(SalesOutboundOrderItem::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());
        erpOrderService.validateAndConfirmOutbound(orderIds, id);

        // 4. 区域预占释放过账（区域账；仓库可用已由批次预留维护，不再对快照做 delta）
        StockPostingResult result = doStockPosting(order, items);

        // 5. 更新出库单状态
        salesOutboundService.updateToConfirmed(id, result.getPostingId());

        log.info("Confirmed sales outbound order via facade, id={}, postingId={}", id, result.getPostingId());
        return Collections.emptyList();
    }

    /**
     * 校验库存是否充足（仓库可用 + 区域预占）
     */
    private List<StockShortageVO> checkStockSufficiency(SalesOutboundOrder order, List<SalesOutboundOrderItem> items) {
        // 按 SKU 汇总需求数量
        Map<String, Integer> requiredMap = items.stream()
                .collect(Collectors.groupingBy(
                        SalesOutboundOrderItem::getSkuCode,
                        Collectors.summingInt(SalesOutboundOrderItem::getQuantity)
                ));

        // 查询仓库库存
        List<String> skuCodes = new ArrayList<>(requiredMap.keySet());
        Map<String, Integer> stockMap = physicalInventoryService.getAllocatableQuantityMap(
                order.getErpTenantId(), order.getWarehouseId(), skuCodes);

        // 获取区域 ID
        Long regionId = platformRegionMappingService.getRegionIdByPlatform(order.getPlatform());

        // 检查不足的 SKU
        List<StockShortageVO> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requiredMap.entrySet()) {
            String skuCode = entry.getKey();
            int required = entry.getValue();

            // 1. 检查仓库可用库存
            int available = stockMap.getOrDefault(skuCode, 0);
            if (available < required) {
                StockShortageVO shortage = new StockShortageVO();
                shortage.setSkuCode(skuCode);
                shortage.setSkuName(skuCode);
                shortage.setRequiredQty(required);
                shortage.setAvailableQty(available);
                shortage.setShortage(required - available);
                shortages.add(shortage);
            }

            // 2. 检查区域预占是否充足
            if (regionId != null) {
                int reserved = regionInventoryService.getReservedQuantity(order.getErpTenantId(), regionId, skuCode);
                if (reserved < required) {
                    StockShortageVO shortage = new StockShortageVO();
                    shortage.setSkuCode(skuCode);
                    shortage.setSkuName(skuCode + " (区域预占不足)");
                    shortage.setRequiredQty(required);
                    shortage.setAvailableQty(reserved);
                    shortage.setShortage(required - reserved);
                    shortages.add(shortage);
                }
            }
        }

        return shortages;
    }

    /**
     * 校验并占用订单
     */
    private void validateAndAllocateOrders(SalesOutboundDTO dto) {
        List<Long> orderIds = dto.getItems().stream()
                .map(SalesOutboundItemDTO::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());

        if (!orderIds.isEmpty()) {
            // 使用乐观锁占用订单
            erpOrderService.allocateForOutbound(orderIds, dto.getPlatform());
        }
    }

    /**
     * 执行库存过账（双 Item：区域释放 + 仓库扣减）
     */
    private StockPostingResult doStockPosting(SalesOutboundOrder order, List<SalesOutboundOrderItem> items) {
        // 获取区域 ID
        Long regionId = platformRegionMappingService.getRegionIdByPlatform(order.getPlatform());
        Assert.notNull(regionId, "平台 " + order.getPlatform() + " 未配置区域映射");

        // 按 SKU 汇总出库数量
        Map<String, Integer> skuQuantityMap = items.stream()
                .collect(Collectors.groupingBy(
                        SalesOutboundOrderItem::getSkuCode,
                        Collectors.summingInt(SalesOutboundOrderItem::getQuantity)
                ));

        List<StockPostingItemDTO> postingItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String skuCode = entry.getKey();
            Integer quantity = entry.getValue();

            // 区域预占释放（仓库可用扣减已改由“确认时批次预留”维护，不再对快照做 delta，见方案A）
            postingItems.add(StockPostingItemDTO.builder()
                    .warehouseId(0L)
                    .regionId(regionId)
                    .erpTenantId(order.getErpTenantId())
                    .skuCode(skuCode)
                    .bucket(StockBucket.RESERVED)
                    .direction(StockDirection.OUT)
                    .quantity(quantity)
                    .build());
        }

        StockPostingDTO postingDTO = StockPostingDTO.builder()
                .postingType(PostingType.SALES_SHIP)
                .sourceType(SourceType.SALES_OUTBOUND.name())
                .sourceId(order.getId())
                .sourceNo(order.getOutboundNo())
                .warehouseId(order.getWarehouseId())
                .regionId(regionId)
                .erpTenantId(order.getErpTenantId())
                .items(postingItems)
                .build();

        return stockPostingService.post(postingDTO);
    }

}
