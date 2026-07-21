package com.erp.admin.order.service;

import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.model.enums.OutboundStatus;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.enums.SourceType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.service.PlatformRegionMappingService;
import com.erp.admin.wms.service.StockPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单预占服务
 * FBS 订单预占/释放的统一入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReservationService {

    private final PlatformRegionMappingService platformRegionMappingService;
    private final StockPostingService stockPostingService;
    private final ErpOrderItemMapper orderItemMapper;
    private final SkuMappingService skuMappingService;

    /**
     * 活跃状态集合——需要预占的订单状态
     */
    private static final Set<String> ACTIVE_STATUSES = new HashSet<>(Arrays.asList(
            ErpOrderStatusEnum.READY_TO_SHIP.name(),
            ErpOrderStatusEnum.SHIPPED.name()
    ));

    /**
     * 新订单入库时调用：判断是否需要预占
     * <p>
     * 仅在 upsertOrder 的 insert 分支调用。
     * 条件：FBS + erpStatus ∈ 活跃状态集
     */
    public void reserveIfNeeded(ErpOrder order) {
        if (!isFbs(order)) {
            return;
        }
        if (!ACTIVE_STATUSES.contains(order.getErpStatus())) {
            log.debug("订单 {} 状态 {} 不在活跃集，跳过预占", order.getId(), order.getErpStatus());
            return;
        }
        doReserve(order);
    }

    /**
     * 订单状态变更时调用：判断是否需要释放预占
     * <p>
     * 在 upsertOrder 的 update 分支和状态同步中调用。
     * 条件：FBS + 旧状态 ∈ 活跃集 + 新状态 ∉ 活跃集 + outbound_status ≠ COMPLETED
     */
    public void handleStatusChange(ErpOrder order, String oldErpStatus, String newErpStatus) {
        if (!isFbs(order)) {
            return;
        }
        if (oldErpStatus == null || oldErpStatus.equals(newErpStatus)) {
            return;
        }
        boolean wasActive = ACTIVE_STATUSES.contains(oldErpStatus);
        boolean isActive = ACTIVE_STATUSES.contains(newErpStatus);

        if (wasActive && !isActive) {
            // 活跃 → 非活跃：检查是否需要释放
            String outboundStatus = order.getOutboundStatus();
            if (OutboundStatus.COMPLETED.name().equals(outboundStatus)) {
                log.info("订单 {} 已通过出库单完成，跳过释放", order.getId());
                return;
            }
            if (OutboundStatus.ALLOCATED.name().equals(outboundStatus)) {
                log.warn("订单 {} 状态变更为 {} 但已在出库单中(ALLOCATED)，释放预占，请运营检查出库单",
                        order.getId(), newErpStatus);
            }
            doRelease(order);
        }
    }

    /**
     * 执行预占过账
     */
    private void doReserve(ErpOrder order) {
        Long regionId = resolveRegionId(order);
        if (regionId == null) return;

        Long erpTenantId = resolveErpTenantId(order);
        if (erpTenantId == null) return;

        List<ErpOrderItem> items = getItems(order);
        Map<String, String> skuCodeMap = resolveSkuCodes(items);
        List<StockPostingItemDTO> postingItems = new ArrayList<>();

        for (ErpOrderItem item : items) {
            String skuCode = resolveItemSkuCode(item, skuCodeMap);
            if (skuCode == null) {
                log.warn("订单 {} item(id={}, platformItemId={}) 无 SKU，跳过预占",
                        order.getId(), item.getId(), item.getPlatformItemId());
                continue;
            }
            postingItems.add(StockPostingItemDTO.builder()
                    .warehouseId(0L)
                    .regionId(regionId)
                    .erpTenantId(erpTenantId)
                    .skuCode(skuCode)
                    .bucket(StockBucket.RESERVED)
                    .direction(StockDirection.IN)
                    .quantity(item.getQuantity())
                    .build());
        }

        if (postingItems.isEmpty()) {
            log.warn("订单 {} 无有效 SKU 映射的 item，跳过预占", order.getId());
            return;
        }

        StockPostingDTO dto = StockPostingDTO.builder()
                .postingType(PostingType.SALES_RESERVE)
                .sourceType(SourceType.ORDER.name())
                .sourceId(order.getId())
                .regionId(regionId)
                .items(postingItems)
                .build();

        stockPostingService.post(dto);
        log.info("订单 {} 执行预占：region={}, items={}",
                order.getId(), regionId,
                postingItems.stream().map(i -> i.getSkuCode() + "×" + i.getQuantity())
                        .collect(Collectors.joining(", ")));
    }

    /**
     * 执行释放过账
     */
    private void doRelease(ErpOrder order) {
        boolean hasReserved = stockPostingService.existsBySourceAndType(
                SourceType.ORDER.name(), order.getId(), PostingType.SALES_RESERVE.name());
        if (!hasReserved) {
            log.warn("订单 {} 无预占记录（历史数据），跳过释放", order.getId());
            return;
        }

        Long regionId = resolveRegionId(order);
        if (regionId == null) return;

        Long erpTenantId = resolveErpTenantId(order);
        if (erpTenantId == null) return;

        List<ErpOrderItem> items = getItems(order);
        Map<String, String> skuCodeMap = resolveSkuCodes(items);
        List<StockPostingItemDTO> postingItems = new ArrayList<>();

        for (ErpOrderItem item : items) {
            String skuCode = resolveItemSkuCode(item, skuCodeMap);
            if (skuCode == null) {
                log.warn("订单 {} item(id={}, platformItemId={}) 无 SKU，跳过释放",
                        order.getId(), item.getId(), item.getPlatformItemId());
                continue;
            }
            postingItems.add(StockPostingItemDTO.builder()
                    .warehouseId(0L)
                    .regionId(regionId)
                    .erpTenantId(erpTenantId)
                    .skuCode(skuCode)
                    .bucket(StockBucket.RESERVED)
                    .direction(StockDirection.OUT)
                    .quantity(item.getQuantity())
                    .build());
        }

        if (postingItems.isEmpty()) {
            log.warn("订单 {} 无有效 SKU 映射的 item，跳过释放", order.getId());
            return;
        }

        StockPostingDTO dto = StockPostingDTO.builder()
                .postingType(PostingType.SALES_RELEASE)
                .sourceType(SourceType.ORDER.name())
                .sourceId(order.getId())
                .regionId(regionId)
                .items(postingItems)
                .build();

        stockPostingService.post(dto);
        log.info("订单 {} 执行释放：region={}, items={}",
                order.getId(), regionId,
                postingItems.stream().map(i -> i.getSkuCode() + "×" + i.getQuantity())
                        .collect(Collectors.joining(", ")));
    }

    private boolean isFbs(ErpOrder order) {
        String fulfillmentType = order.getFulfillmentType();
        return fulfillmentType != null && fulfillmentType.toLowerCase().contains("fbs");
    }

    private Long resolveRegionId(ErpOrder order) {
        Long regionId = platformRegionMappingService.getRegionIdByPlatform(order.getPlatform());
        if (regionId == null) {
            log.warn("平台 {} 未配置区域映射，跳过库存操作", order.getPlatform());
        }
        return regionId;
    }

    /**
     * 解析订单所属货主（区域库存按货主隔离，过账必须携带）。
     * 订单同步/状态变更均运行在 {@code TenantContext.runAs(shop.tenantId)} 内，故取当前租户即订单货主。
     */
    private Long resolveErpTenantId(ErpOrder order) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || TenantContext.BLOCK_TENANT_ID.equals(tenantId)) {
            log.warn("订单 {} 无法确定货主(当前租户={}),跳过区域预占/释放", order.getId(), tenantId);
            return null;
        }
        return tenantId;
    }

    /**
     * 仅为「明细自带 sku_code 为空」的行按 platformItemId 查 sku_mapping 兜底映射。
     * 当前订单同步已把 sku_code 直接落库、platform_item_id 多为空，故绝大多数情况无需查映射。
     */
    private Map<String, String> resolveSkuCodes(List<ErpOrderItem> items) {
        Set<String> platformItemIds = items.stream()
                .filter(i -> !org.springframework.util.StringUtils.hasText(i.getSkuCode()))
                .map(ErpOrderItem::getPlatformItemId)
                .filter(org.springframework.util.StringUtils::hasText)
                .collect(Collectors.toSet());
        if (platformItemIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return skuMappingService.getSkuCodeMapByPlatformItemIds(platformItemIds);
    }

    /**
     * 解析明细 SKU 编码：优先订单明细自带的 sku_code，缺失时回退 platformItemId → sku_mapping 映射。
     */
    private String resolveItemSkuCode(ErpOrderItem item, Map<String, String> skuCodeMap) {
        if (org.springframework.util.StringUtils.hasText(item.getSkuCode())) {
            return item.getSkuCode();
        }
        return skuCodeMap.get(item.getPlatformItemId());
    }

    /**
     * 获取订单的 items（优先内存，回退 DB 查询）
     */
    private List<ErpOrderItem> getItems(ErpOrder order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            return order.getItems();
        }
        List<ErpOrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        order.setItems(items);
        return items;
    }
}
