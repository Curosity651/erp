package com.erp.admin.wms.facade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.ozon.OzonClient;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.response.analytics.OzonAnalyticsStockItem;
import com.erp.admin.platform.ozon.model.response.product.OzonProductInfoItem;
import com.erp.admin.platform.ozon.model.response.product.OzonProductItem;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.erp.admin.wms.model.entity.FboSyncLog;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.FboSyncStatus;
import com.erp.admin.wms.model.enums.FboSyncType;
import com.erp.admin.wms.model.vo.FboSyncResultVO;
import com.erp.admin.wms.service.FboSyncLogService;
import com.erp.admin.wms.service.InventoryService;
import com.erp.admin.wms.service.PlatformRegionMappingService;
import com.erp.admin.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Ozon FBO 库存同步 Facade
 * <p>
 * 编排 FBO 库存同步流程：
 * 1. 获取商品列表
 * 2. 分批查询 FBO 库存
 * 3. 按 Cluster 聚合库存数据
 * 4. 被动发现并创建仓库（按店铺 + Cluster）
 * 5. 映射 SKU 并更新库存
 * 6. 记录同步日志
 * <p>
 * 数据映射规则：
 * - Ozon Cluster → 系统 Warehouse
 * - 同一 (offer_id, cluster_id) 的库存求和聚合
 * - 每个店铺独立处理，不跨店铺聚合
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OzonFboStockSyncFacade {

    private final ShopService shopService;
    private final OzonClient ozonClient;
    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;
    private final SkuMappingService skuMappingService;
    private final FboSyncLogService fboSyncLogService;
    private final PlatformRegionMappingService platformRegionMappingService;
	private final CredentialService credentialService;

    private static final String PLATFORM = PlatformEnum.Ozon.code();

    /**
     * 同步所有启用的 Ozon 店铺
     *
     * @return 同步结果列表
     */
    public List<FboSyncResultVO> syncAllShops(String syncType) {
        List<Shop> shops = shopService.listEnabledByPlatform(PLATFORM);
        if (CollectionUtils.isEmpty(shops)) {
            log.info("没有启用的 Ozon 店铺，跳过 FBO 库存同步");
            return new ArrayList<>();
        }

        List<FboSyncResultVO> results = new ArrayList<>();
        for (Shop shop : shops) {
            try {
                FboSyncResultVO result = TenantContext.runAs(shop.getTenantId(), () -> syncByShop(shop, syncType));
                results.add(result);
            } catch (Exception e) {
                log.error("店铺 FBO 库存同步失败: shopId={}, error={}", shop.getId(), e.getMessage(), e);
            }
        }
        return results;
    }

    /**
     * 同步指定店铺
     *
     * @param shopId 店铺ID
     * @return 同步结果
     */
    public FboSyncResultVO syncByShopId(Long shopId) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在: " + shopId);
        }
        if (!PLATFORM.equalsIgnoreCase(shop.getPlatform())) {
            throw new IllegalArgumentException("仅支持 Ozon 店铺");
        }
        return syncByShop(shop, FboSyncType.MANUAL.getCode());
    }

    /**
     * 执行单个店铺的同步
     */
    private FboSyncResultVO syncByShop(Shop shop, String syncType) {
        long startTime = System.currentTimeMillis();

        // 创建同步日志
        FboSyncLog syncLog = fboSyncLogService.createLog(PLATFORM, shop.getId(), syncType);

        // 统计变量
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int unmappedCount = 0;
        int autoCreatedWarehouseCount = 0;
        List<FboSyncLog.AutoCreatedWarehouse> autoCreatedWarehouses = new ArrayList<>();
        List<FboSyncLog.FboSyncFailDetail> failDetails = new ArrayList<>();

        try {
            // 1. 解析凭证
            OzonCredential credential = credentialService.parseCredential(shop);

            // 2. 获取商品列表
            List<OzonProductItem> products = ozonClient.getProducts(credential);
            if (CollectionUtils.isEmpty(products)) {
                log.info("店铺无商品，跳过同步: shopId={}", shop.getId());
                updateSyncLog(syncLog, FboSyncStatus.SUCCESS.getCode(), null,
                        0, 0, 0, 0, 0, null, null, startTime);
                return buildResult(syncLog);
            }

            // 3. 提取 product_id 列表
            List<Long> productIds = products.stream()
                    .map(OzonProductItem::getProductId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(productIds)) {
                log.info("无有效商品 ID，跳过同步: shopId={}", shop.getId());
                updateSyncLog(syncLog, FboSyncStatus.SUCCESS.getCode(), null,
                        0, 0, 0, 0, 0, null, null, startTime);
                return buildResult(syncLog);
            }

            // 4. 通过 product_id 获取商品详情（含 SKU）
            List<OzonProductInfoItem> productInfoList = ozonClient.getProductInfoListBatch(credential, productIds);
            List<Long> skus = productInfoList.stream()
                    .map(OzonProductInfoItem::getSku)
                    .filter(sku -> sku != null && sku > 0)
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(skus)) {
                log.info("无有效 SKU，跳过同步: shopId={}", shop.getId());
                updateSyncLog(syncLog, FboSyncStatus.SUCCESS.getCode(), null,
                        0, 0, 0, 0, 0, null, null, startTime);
                return buildResult(syncLog);
            }

            // 5. 通过 SKU 查询 FBO 库存
            List<OzonAnalyticsStockItem> stockItems = ozonClient.getFboStocksBatch(credential, skus);

            if (CollectionUtils.isEmpty(stockItems)) {
                log.info("无 FBO 库存数据: shopId={}", shop.getId());
                updateSyncLog(syncLog, FboSyncStatus.SUCCESS.getCode(), null,
                        0, 0, 0, 0, 0, null, null, startTime);
                return buildResult(syncLog);
            }

            // 6. 按 (offer_id, cluster_id) 聚合库存
            Map<ClusterStockKey, ClusterStockValue> aggregatedStocks = aggregateByCluster(stockItems);
            totalCount = aggregatedStocks.size();

            if (aggregatedStocks.isEmpty()) {
                log.info("聚合后无有效库存数据: shopId={}", shop.getId());
                updateSyncLog(syncLog, FboSyncStatus.SUCCESS.getCode(), null,
                        0, 0, 0, 0, 0, null, null, startTime);
                return buildResult(syncLog);
            }

            // 7. 构建 offer_id -> sku_code 映射
            List<String> offerIds = aggregatedStocks.keySet().stream()
                    .map(ClusterStockKey::getOfferId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, String> skuCodeMap = skuMappingService.getSkuCodeMapByPlatformItemIds(offerIds);

            // 8. 按 Cluster 分组处理
            Map<Long, List<Map.Entry<ClusterStockKey, ClusterStockValue>>> clusterGrouped = aggregatedStocks.entrySet().stream()
                    .collect(Collectors.groupingBy(e -> e.getKey().getClusterId()));

            // 记录已创建的仓库，避免重复计数
            Map<Long, Warehouse> createdWarehouseCache = new HashMap<>();

            // 预查询平台对应的区域 ID，用于新建 FBO 仓库时设置
            Long regionId = platformRegionMappingService.getRegionIdByPlatform(PLATFORM);

            LocalDateTime syncTime = LocalDateTime.now();

            for (Map.Entry<Long, List<Map.Entry<ClusterStockKey, ClusterStockValue>>> clusterEntry : clusterGrouped.entrySet()) {
                Long clusterId = clusterEntry.getKey();
                List<Map.Entry<ClusterStockKey, ClusterStockValue>> items = clusterEntry.getValue();

                // 获取 cluster 名称（取第一个即可，同一 cluster 名称相同）
                String clusterName = items.get(0).getValue().getClusterName();
                // 仓库名称格式：店铺名 - Cluster名称
                String warehouseName = shop.getName() + " - " + clusterName;

                // 获取或创建仓库（按店铺 + Cluster）
                Warehouse warehouse = createdWarehouseCache.get(clusterId);

                if (warehouse == null) {
                    Warehouse existing = warehouseService.getByPlatformShopAndWarehouseId(
                            PLATFORM, shop.getId(), String.valueOf(clusterId));
                    if (existing == null) {
                        warehouse = warehouseService.getOrCreateFboWarehouse(
                                PLATFORM, shop.getId(), String.valueOf(clusterId), warehouseName, regionId);
                        autoCreatedWarehouseCount++;
                        autoCreatedWarehouses.add(new FboSyncLog.AutoCreatedWarehouse(
                                warehouse.getWarehouseCode(),
                                warehouse.getWarehouseName(),
                                warehouse.getPlatformWarehouseId()
                        ));
                    } else {
                        warehouse = existing;
                    }
                    createdWarehouseCache.put(clusterId, warehouse);
                }

                // 检查仓库状态
                if (warehouse.getStatus() != 1) {
                    log.debug("仓库已停用，跳过库存更新: warehouseId={}, clusterId={}", warehouse.getId(), clusterId);
                    continue;
                }

                // 更新库存
                for (Map.Entry<ClusterStockKey, ClusterStockValue> stockEntry : items) {
                    ClusterStockKey key = stockEntry.getKey();
                    ClusterStockValue value = stockEntry.getValue();

                    String skuCode = skuCodeMap.get(key.getOfferId());
                    if (skuCode == null) {
                        unmappedCount++;
                        failDetails.add(new FboSyncLog.FboSyncFailDetail(
                                key.getOfferId(), "UNMAPPED", "SKU 未映射"));
                        continue;
                    }

                    try {
                        inventoryService.syncFboStock(warehouse.getId(), skuCode, value.getAvailableStockCount(), syncTime);
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        failDetails.add(new FboSyncLog.FboSyncFailDetail(
                                key.getOfferId(), "UPDATE_ERROR", e.getMessage()));
                        log.warn("库存更新失败: offerId={}, clusterId={}, error={}",
                                key.getOfferId(), clusterId, e.getMessage());
                    }
                }
            }

            // 更新同步日志
            String status = (failCount == 0 && unmappedCount == 0)
                    ? FboSyncStatus.SUCCESS.getCode()
                    : FboSyncStatus.PARTIAL.getCode();
            updateSyncLog(syncLog, status, null,
                    totalCount, successCount, failCount, unmappedCount,
                    autoCreatedWarehouseCount, autoCreatedWarehouses, failDetails, startTime);

        } catch (Exception e) {
            log.error("FBO 库存同步异常: shopId={}", shop.getId(), e);
            updateSyncLog(syncLog, FboSyncStatus.FAILED.getCode(), e.getMessage(),
                    totalCount, successCount, failCount, unmappedCount,
                    autoCreatedWarehouseCount, autoCreatedWarehouses, failDetails, startTime);
        }

        return buildResult(syncLog);
    }

    /**
     * 按 (offer_id, cluster_id) 聚合库存数据
     * <p>
     * 同一个 SKU 在同一个 Cluster 内可能分布在多个仓库，需要求和聚合。
     * 过滤掉 cluster_id = 0 的无效记录。
     *
     * @param stockItems 原始库存数据
     * @return 聚合后的库存数据
     */
    private Map<ClusterStockKey, ClusterStockValue> aggregateByCluster(List<OzonAnalyticsStockItem> stockItems) {
        Map<ClusterStockKey, ClusterStockValue> aggregatedStocks = new HashMap<>();

        for (OzonAnalyticsStockItem item : stockItems) {
            // 过滤无效 cluster
            if (item.getClusterId() == null || item.getClusterId() == 0) {
                continue;
            }

            // 过滤空的 offer_id
            if (item.getOfferId() == null || item.getOfferId().isEmpty()) {
                continue;
            }

            ClusterStockKey key = new ClusterStockKey(item.getOfferId(), item.getClusterId());
            ClusterStockValue value = aggregatedStocks.computeIfAbsent(key,
                    k -> new ClusterStockValue(item.getClusterName(), 0));

            // 累加库存
            int quantity = Optional.ofNullable(item.getAvailableStockCount()).orElse(0);
            value.addStock(quantity);
        }

        return aggregatedStocks;
    }

    private void updateSyncLog(FboSyncLog syncLog, String status, String errorMessage,
                               int totalCount, int successCount, int failCount, int unmappedCount,
                               int autoCreatedWarehouseCount,
                               List<FboSyncLog.AutoCreatedWarehouse> autoCreatedWarehouses,
                               List<FboSyncLog.FboSyncFailDetail> failDetails,
                               long startTime) {
        syncLog.setSyncStatus(status);
        syncLog.setErrorMessage(errorMessage);
        syncLog.setTotalCount(totalCount);
        syncLog.setSuccessCount(successCount);
        syncLog.setFailCount(failCount);
        syncLog.setUnmappedCount(unmappedCount);
        syncLog.setAutoCreatedWarehouseCount(autoCreatedWarehouseCount);
        syncLog.setAutoCreatedWarehouses(autoCreatedWarehouses);
        syncLog.setFailDetails(CollectionUtils.isEmpty(failDetails) ? null : failDetails);
        syncLog.setDuration((int) (System.currentTimeMillis() - startTime));
        fboSyncLogService.updateById(syncLog);
    }

    private FboSyncResultVO buildResult(FboSyncLog syncLog) {
        return FboSyncResultVO.builder()
                .logNo(syncLog.getLogNo())
                .totalCount(syncLog.getTotalCount())
                .successCount(syncLog.getSuccessCount())
                .failCount(syncLog.getFailCount())
                .unmappedCount(syncLog.getUnmappedCount())
                .syncStatus(syncLog.getSyncStatus())
                .autoCreatedWarehouseCount(syncLog.getAutoCreatedWarehouseCount())
                .build();
    }

    // ==================== 内部类 ====================

    /**
     * Cluster 库存聚合 Key
     */
    private static class ClusterStockKey {
        private final String offerId;
        private final Long clusterId;

        public ClusterStockKey(String offerId, Long clusterId) {
            this.offerId = offerId;
            this.clusterId = clusterId;
        }

        public String getOfferId() {
            return offerId;
        }

        public Long getClusterId() {
            return clusterId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClusterStockKey that = (ClusterStockKey) o;
            return Objects.equals(offerId, that.offerId) && Objects.equals(clusterId, that.clusterId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(offerId, clusterId);
        }
    }

    /**
     * Cluster 库存聚合 Value
     */
    private static class ClusterStockValue {
        private final String clusterName;
        private int availableStockCount;

        public ClusterStockValue(String clusterName, int availableStockCount) {
            this.clusterName = clusterName;
            this.availableStockCount = availableStockCount;
        }

        public String getClusterName() {
            return clusterName;
        }

        public int getAvailableStockCount() {
            return availableStockCount;
        }

        public void addStock(int quantity) {
            this.availableStockCount += quantity;
        }
    }

}
