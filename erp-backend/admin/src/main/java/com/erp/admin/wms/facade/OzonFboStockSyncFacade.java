package com.erp.admin.wms.facade;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.ozon.OzonClient;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.response.analytics.OzonAnalyticsStockItem;
import com.erp.admin.platform.ozon.model.response.product.OzonProductInfoItem;
import com.erp.admin.platform.ozon.model.response.product.OzonProductItem;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.erp.admin.wms.model.entity.FboInventorySnapshot;
import com.erp.admin.wms.model.entity.FboSyncLog;
import com.erp.admin.wms.model.enums.FboSyncStatus;
import com.erp.admin.wms.model.enums.FboSyncType;
import com.erp.admin.wms.model.vo.FboSyncResultVO;
import com.erp.admin.wms.service.FboInventorySnapshotService;
import com.erp.admin.wms.service.FboSyncLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Owner-side Ozon FBO complete snapshot synchronization. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OzonFboStockSyncFacade {

    private static final String PLATFORM = PlatformEnum.Ozon.code();
    private static final Duration LOCK_TTL = Duration.ofMinutes(20);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final ShopService shopService;
    private final OzonClient ozonClient;
    private final SkuMappingService skuMappingService;
    private final FboSyncLogService fboSyncLogService;
    private final FboInventorySnapshotService snapshotService;
    private final CredentialService credentialService;
    private final StringRedisTemplate redisTemplate;

    /** Scheduler-only cross-tenant scan. Each shop is executed in its owner context. */
    public List<FboSyncResultVO> syncAllShops(String syncType) {
        List<Shop> shops = shopService.listEnabledByPlatform(PLATFORM);
        List<FboSyncResultVO> results = new ArrayList<>();
        for (Shop shop : shops) {
            try {
                results.add(TenantContext.runAs(shop.getTenantId(), () -> syncByShop(shop, syncType)));
            }
            catch (Exception ex) {
                log.error("Scheduled FBO sync failed, tenantId={}, shopId={}",
                        shop.getTenantId(), shop.getId(), ex);
            }
        }
        return results;
    }

    /** Manual all-store synchronization is restricted to the current owner. */
    public List<FboSyncResultVO> syncCurrentTenantShops(String syncType) {
        requireOwnerTenant();
        List<FboSyncResultVO> results = new ArrayList<>();
        for (Shop shop : shopService.listCurrentTenantEnabledByPlatform(PLATFORM)) {
            results.add(syncByShop(shop, syncType));
        }
        return results;
    }

    public FboSyncResultVO syncByShopId(Long shopId) {
        Long tenantId = requireOwnerTenant();
        Shop shop = shopService.getById(shopId);
        if (shop == null || !Objects.equals(shop.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("Shop does not exist or is outside the current owner scope");
        }
        if (!PLATFORM.equalsIgnoreCase(shop.getPlatform())) {
            throw new IllegalArgumentException("Only Ozon FBO inventory is supported");
        }
        return syncByShop(shop, FboSyncType.MANUAL.getCode());
    }

    private FboSyncResultVO syncByShop(Shop shop, String syncType) {
        Long tenantId = shop.getTenantId();
        String lockKey = "wms:fbo-sync:" + tenantId + ":" + PLATFORM + ":" + shop.getId();
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalStateException("This shop is already being synchronized");
        }

        long startTime = System.currentTimeMillis();
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        FboSyncLog syncLog = fboSyncLogService.createLog(tenantId, PLATFORM, shop.getId(), syncType, batchNo);
        try {
            SyncPayload payload = fetchCompletePayload(shop, tenantId, batchNo);
            snapshotService.replaceShopSnapshot(tenantId, PLATFORM, shop.getId(), payload.rows);

            String status = payload.unmappedCount == 0
                    ? FboSyncStatus.SUCCESS.getCode() : FboSyncStatus.PARTIAL.getCode();
            updateSyncLog(syncLog, status, null, payload.sourceCount, payload.totalCount,
                    payload.rows.size(), 0, payload.unmappedCount, payload.snapshotQuantity,
                    payload.failDetails, startTime);
            return buildResult(syncLog);
        }
        catch (Exception ex) {
            log.error("FBO snapshot sync failed, tenantId={}, shopId={}", tenantId, shop.getId(), ex);
            updateSyncLog(syncLog, FboSyncStatus.FAILED.getCode(), ex.getMessage(),
                    0, 0, 0, 1, 0, 0, Collections.emptyList(), startTime);
            return buildResult(syncLog);
        }
        finally {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockToken);
        }
    }

    /** Fetches and maps the whole shop before the database snapshot is touched. */
    private SyncPayload fetchCompletePayload(Shop shop, Long tenantId, String batchNo) {
        OzonCredential credential = credentialService.parseCredential(shop);
        List<OzonProductItem> products = ozonClient.getProducts(credential);
        if (CollectionUtils.isEmpty(products)) {
            return new SyncPayload();
        }

        List<Long> productIds = products.stream().map(OzonProductItem::getProductId)
                .filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return new SyncPayload();
        }
        List<OzonProductInfoItem> productInfo = ozonClient.getProductInfoListBatch(credential, productIds);
        List<Long> platformSkus = productInfo.stream().map(OzonProductInfoItem::getSku)
                .filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
        if (platformSkus.isEmpty()) {
            return new SyncPayload();
        }

        List<OzonAnalyticsStockItem> source = ozonClient.getFboStocksBatch(credential, platformSkus);
        Map<ClusterStockKey, ClusterStockValue> aggregated = aggregateByCluster(source);
        Map<String, String> skuMappings = skuMappingService.getSkuCodeMapByPlatformItemIds(
                aggregated.keySet().stream().map(ClusterStockKey::getOfferId).distinct().collect(Collectors.toList()));

        SyncPayload payload = new SyncPayload();
        payload.sourceCount = source == null ? 0 : source.size();
        payload.totalCount = aggregated.size();
        LocalDateTime syncedAt = LocalDateTime.now();
        Map<String, FboInventorySnapshot> finalRows = new LinkedHashMap<>();
        for (Map.Entry<ClusterStockKey, ClusterStockValue> entry : aggregated.entrySet()) {
            ClusterStockKey key = entry.getKey();
            ClusterStockValue value = entry.getValue();
            String skuCode = skuMappings.get(key.offerId);
            if (skuCode == null) {
                payload.unmappedCount++;
                payload.failDetails.add(new FboSyncLog.FboSyncFailDetail(
                        key.offerId, "UNMAPPED", "SKU mapping is missing"));
                continue;
            }
            String rowKey = key.clusterId + "\u0000" + skuCode;
            FboInventorySnapshot row = finalRows.get(rowKey);
            if (row == null) {
                row = new FboInventorySnapshot();
                row.setTenantId(tenantId);
                row.setPlatform(PLATFORM);
                row.setShopId(shop.getId());
                row.setPlatformWarehouseId(String.valueOf(key.clusterId));
                row.setPlatformWarehouseName(value.clusterName);
                row.setPlatformItemId(key.offerId);
                row.setSkuCode(skuCode);
                row.setQuantity(0);
                row.setSyncBatchNo(batchNo);
                row.setSyncedAt(syncedAt);
                finalRows.put(rowKey, row);
            }
            row.setQuantity(row.getQuantity() + value.availableStockCount);
        }
        payload.rows.addAll(finalRows.values());
        payload.snapshotQuantity = payload.rows.stream().mapToInt(r -> Optional.ofNullable(r.getQuantity()).orElse(0)).sum();
        return payload;
    }

    private Map<ClusterStockKey, ClusterStockValue> aggregateByCluster(List<OzonAnalyticsStockItem> stockItems) {
        Map<ClusterStockKey, ClusterStockValue> result = new HashMap<>();
        if (stockItems == null) {
            return result;
        }
        for (OzonAnalyticsStockItem item : stockItems) {
            if (item.getClusterId() == null || item.getClusterId() == 0
                    || item.getOfferId() == null || item.getOfferId().isEmpty()) {
                continue;
            }
            ClusterStockKey key = new ClusterStockKey(item.getOfferId(), item.getClusterId());
            ClusterStockValue value = result.computeIfAbsent(key,
                    ignored -> new ClusterStockValue(item.getClusterName()));
            value.availableStockCount += Optional.ofNullable(item.getAvailableStockCount()).orElse(0);
        }
        return result;
    }

    private void updateSyncLog(FboSyncLog logEntity, String status, String error,
            int sourceCount, int totalCount, int successCount, int failCount, int unmappedCount,
            int snapshotQuantity, List<FboSyncLog.FboSyncFailDetail> failDetails, long startTime) {
        logEntity.setSyncStatus(status);
        logEntity.setErrorMessage(error);
        logEntity.setSourceCount(sourceCount);
        logEntity.setTotalCount(totalCount);
        logEntity.setSuccessCount(successCount);
        logEntity.setFailCount(failCount);
        logEntity.setUnmappedCount(unmappedCount);
        logEntity.setSnapshotQuantity(snapshotQuantity);
        logEntity.setAutoCreatedWarehouseCount(0);
        logEntity.setAutoCreatedWarehouses(null);
        logEntity.setFailDetails(CollectionUtils.isEmpty(failDetails) ? null : failDetails);
        logEntity.setDuration((int) (System.currentTimeMillis() - startTime));
        fboSyncLogService.updateById(logEntity);
    }

    private FboSyncResultVO buildResult(FboSyncLog logEntity) {
        return FboSyncResultVO.builder().logNo(logEntity.getLogNo())
                .totalCount(logEntity.getTotalCount()).successCount(logEntity.getSuccessCount())
                .failCount(logEntity.getFailCount()).unmappedCount(logEntity.getUnmappedCount())
                .syncStatus(logEntity.getSyncStatus()).autoCreatedWarehouseCount(0).build();
    }

    private Long requireOwnerTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || TenantContext.BLOCK_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException("FBO inventory synchronization is owner-only");
        }
        return tenantId;
    }

    private static final class SyncPayload {
        private int sourceCount;
        private int totalCount;
        private int unmappedCount;
        private int snapshotQuantity;
        private final List<FboInventorySnapshot> rows = new ArrayList<>();
        private final List<FboSyncLog.FboSyncFailDetail> failDetails = new ArrayList<>();
    }

    private static final class ClusterStockKey {
        private final String offerId;
        private final Long clusterId;
        private ClusterStockKey(String offerId, Long clusterId) {
            this.offerId = offerId;
            this.clusterId = clusterId;
        }
        private String getOfferId() { return offerId; }
        @Override public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof ClusterStockKey)) return false;
            ClusterStockKey that = (ClusterStockKey) value;
            return Objects.equals(offerId, that.offerId) && Objects.equals(clusterId, that.clusterId);
        }
        @Override public int hashCode() { return Objects.hash(offerId, clusterId); }
    }

    private static final class ClusterStockValue {
        private final String clusterName;
        private int availableStockCount;
        private ClusterStockValue(String clusterName) { this.clusterName = clusterName; }
    }
}
