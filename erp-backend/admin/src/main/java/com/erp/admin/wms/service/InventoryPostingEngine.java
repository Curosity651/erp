package com.erp.admin.wms.service;

import com.erp.admin.wms.exception.OptimisticLockException;
import com.erp.admin.wms.mapper.InventoryMapper;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.entity.RegionInventory;
import com.erp.admin.wms.model.entity.StockFlow;
import com.erp.admin.wms.model.entity.StockPosting;
import com.erp.admin.wms.model.entity.StockPostingItem;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存过账引擎
 * 负责更新库存快照、记录流水、乐观锁控制
 * 支持仓库级和区域级两种操作
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryPostingEngine {

    private static final int MAX_RETRY = 3;

    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;
    private final RegionInventoryService regionInventoryService;
    private final StockFlowService stockFlowService;

    /**
     * 执行库存变更
     *
     * @param posting 过账单
     * @param items   过账明细（已包含完整信息）
     */
    public void execute(StockPosting posting, List<StockPostingItem> items) {
        // C-3 修复：乐观锁重试已下沉到每个分组内部（processWarehouseItems/processRegionItems）。
        // 不再对整段 doExecute 做重试——否则某分组失败重跑时，会把已成功分组的增量重复应用一遍。
        doExecute(posting, items);
    }

    private void doExecute(StockPosting posting, List<StockPostingItem> items) {
        List<StockFlow> flowsToInsert = new ArrayList<>();

        // 分离仓库级和区域级 Items
        List<StockPostingItem> warehouseItems = new ArrayList<>();
        List<StockPostingItem> regionItems = new ArrayList<>();

        for (StockPostingItem item : items) {
            Long warehouseId = item.getWarehouseId() != null ? item.getWarehouseId() : 0L;
            Long regionId = item.getRegionId() != null ? item.getRegionId() : 0L;

            if (warehouseId > 0 && regionId == 0) {
                warehouseItems.add(item);
            } else if (warehouseId == 0 && regionId > 0) {
                regionItems.add(item);
            } else {
                throw new IllegalArgumentException(
                        "Invalid posting item: must have exactly one of warehouse_id or region_id > 0, " +
                        "got warehouseId=" + warehouseId + ", regionId=" + regionId);
            }
        }

        // 处理仓库级 Items（现有逻辑）
        if (!warehouseItems.isEmpty()) {
            processWarehouseItems(posting, warehouseItems, flowsToInsert);
        }

        // 处理区域级 Items（新增逻辑）
        if (!regionItems.isEmpty()) {
            processRegionItems(posting, regionItems, flowsToInsert);
        }

        // 批量插入流水
        if (!flowsToInsert.isEmpty()) {
            stockFlowService.saveBatch(flowsToInsert);
        }

        log.info("过账执行完成，postingNo={}，itemCount={}，flowCount={}",
                posting.getPostingNo(), items.size(), flowsToInsert.size());
    }

    /**
     * 处理仓库级 Items（原有逻辑）
     */
    private void processWarehouseItems(StockPosting posting, List<StockPostingItem> items, List<StockFlow> flowsToInsert) {
        // 按 (erpTenantId, warehouseId, skuCode) 分组处理——仓库级库存按货主隔离，
        // 不同货主的同仓同 SKU 必须落到各自的库存行，绝不能合并。
        Map<String, List<StockPostingItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        InventoryPostingEngine::warehouseGroupKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<StockPostingItem>> entry : grouped.entrySet()) {
            List<StockPostingItem> skuItems = entry.getValue();
            StockPostingItem first = skuItems.get(0);
            Long erpTenantId = first.getErpTenantId();
            Long warehouseId = first.getWarehouseId();
            String skuCode = first.getSkuCode();

            // C-3：本分组独立乐观锁重试。每次尝试都重新读取该行最新版本、从新值重算增量，
            // 成功才把本组流水并入总表——避免整段重试把已成功分组的增量重复应用。
            boolean applied = false;
            for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
                Inventory inv = inventoryService.getOrCreate(erpTenantId, warehouseId, skuCode);
                Integer oldVersion = inv.getVersion();
                List<StockFlow> groupFlows = new ArrayList<>();

                for (StockPostingItem item : skuItems) {
                    StockBucket bucket = StockBucket.valueOf(item.getBucket());
                    StockDirection direction = StockDirection.valueOf(item.getDirection());

                    // SCRAP 桶跳过库存快照更新，仅记录流水
                    if (bucket == StockBucket.SCRAP) {
                        groupFlows.add(buildWarehouseFlow(posting, item, 0, 0));
                        continue;
                    }

                    int beforeQty = getBucketQuantity(inv, bucket);
                    int delta = direction == StockDirection.IN
                            ? item.getQuantity()
                            : -item.getQuantity();
                    int afterQty = beforeQty + delta;

                    // 约束校验（不足等业务错误直接抛出，不参与乐观锁重试）
                    validateConstraint(bucket, afterQty, skuCode);

                    // 更新内存快照
                    setBucketQuantity(inv, bucket, afterQty);

                    // 构建本组流水
                    groupFlows.add(buildWarehouseFlow(posting, item, beforeQty, afterQty));
                }

                int affected = inventoryMapper.updateWithVersion(
                        inv.getId(),
                        inv.getAvailableQuantity(),
                        inv.getReservedQuantity(),
                        inv.getInTransitQuantity(),
                        inv.getDamagedQuantity(),
                        oldVersion
                );
                if (affected > 0) {
                    flowsToInsert.addAll(groupFlows);
                    applied = true;
                    break;
                }
                log.warn("仓库过账乐观锁冲突，分组重试 {}/{}，warehouseId={}, skuCode={}",
                        attempt, MAX_RETRY, warehouseId, skuCode);
            }
            if (!applied) {
                throw new OptimisticLockException("库存并发冲突，warehouseId=" + warehouseId + ", skuCode=" + skuCode);
            }
        }
    }

    /**
     * 处理区域级 Items（新增逻辑）
     */
    private void processRegionItems(StockPosting posting, List<StockPostingItem> items, List<StockFlow> flowsToInsert) {
        // 按 (erpTenantId, regionId, skuCode) 分组处理——区域库存按货主隔离，
        // 不同货主的同名 SKU 必须落到各自的库存记录，绝不能合并。
        Map<String, List<StockPostingItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        InventoryPostingEngine::regionGroupKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<StockPostingItem>> entry : grouped.entrySet()) {
            List<StockPostingItem> skuItems = entry.getValue();
            StockPostingItem first = skuItems.get(0);
            Long erpTenantId = first.getErpTenantId();
            Long regionId = first.getRegionId();
            String skuCode = first.getSkuCode();

            // C-3：本分组独立乐观锁重试。每次尝试从最新版本重算增量，成功才并入流水。
            boolean applied = false;
            for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
                RegionInventory regionInv = regionInventoryService.getOrCreate(erpTenantId, regionId, skuCode);
                Integer oldVersion = regionInv.getVersion();
                int currentReserved = regionInv.getReservedQuantity() != null ? regionInv.getReservedQuantity() : 0;
                int currentInTransit = regionInv.getInTransitQuantity() != null ? regionInv.getInTransitQuantity() : 0;
                List<StockFlow> groupFlows = new ArrayList<>();

                for (StockPostingItem item : skuItems) {
                    StockBucket bucket = StockBucket.valueOf(item.getBucket());
                    StockDirection direction = StockDirection.valueOf(item.getDirection());

                    if (bucket != StockBucket.RESERVED && bucket != StockBucket.IN_TRANSIT) {
                        throw new IllegalArgumentException(
                                "区域级操作只支持 RESERVED / IN_TRANSIT 桶，当前桶: " + bucket);
                    }

                    int beforeQty;
                    int delta = direction == StockDirection.IN ? item.getQuantity() : -item.getQuantity();

                    if (bucket == StockBucket.RESERVED) {
                        beforeQty = currentReserved;
                        int afterQty = beforeQty + delta;
                        if (afterQty < 0) {
                            throw new IllegalStateException(String.format(
                                    "区域预占不足，regionId=%d, SKU=%s，当前预占=%d，尝试释放=%d",
                                    regionId, skuCode, beforeQty, item.getQuantity()));
                        }
                        currentReserved = afterQty;
                        groupFlows.add(buildRegionFlow(posting, item, beforeQty, afterQty));
                    } else {
                        // IN_TRANSIT
                        beforeQty = currentInTransit;
                        int afterQty = beforeQty + delta;
                        if (afterQty < 0) {
                            throw new IllegalStateException(String.format(
                                    "区域在途不足，regionId=%d, SKU=%s，当前在途=%d，尝试释放=%d",
                                    regionId, skuCode, beforeQty, item.getQuantity()));
                        }
                        currentInTransit = afterQty;
                        groupFlows.add(buildRegionFlow(posting, item, beforeQty, afterQty));
                    }
                }

                // 乐观锁更新区域库存（预占 + 在途一起更新）
                boolean success = regionInventoryService.updateWithVersion(
                        regionInv.getId(), currentReserved, currentInTransit, oldVersion);
                if (success) {
                    flowsToInsert.addAll(groupFlows);
                    applied = true;
                    break;
                }
                log.warn("区域过账乐观锁冲突，分组重试 {}/{}，regionId={}, skuCode={}",
                        attempt, MAX_RETRY, regionId, skuCode);
            }
            if (!applied) {
                throw new OptimisticLockException("区域库存并发冲突，regionId=" + regionId + ", skuCode=" + skuCode);
            }
        }
    }

    /**
     * 区域级过账分组键 = (货主, 区域, SKU)。货主维入键，保证不同货主的同名 SKU
     * 落到各自的区域库存记录，绝不合并。
     */
    public static String regionGroupKey(StockPostingItem item) {
        return item.getErpTenantId() + ":" + item.getRegionId() + ":" + item.getSkuCode();
    }

    /**
     * 仓库级过账分组键 = (货主, 仓库, SKU)。货主维入键，保证不同货主的同仓同 SKU
     * 落到各自的库存行，绝不合并。
     */
    public static String warehouseGroupKey(StockPostingItem item) {
        return item.getErpTenantId() + ":" + item.getWarehouseId() + ":" + item.getSkuCode();
    }

    private int getBucketQuantity(Inventory inv, StockBucket bucket) {
        switch (bucket) {
            case AVAILABLE:
                return inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : 0;
            case RESERVED:
                return inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
            case IN_TRANSIT:
                return inv.getInTransitQuantity() != null ? inv.getInTransitQuantity() : 0;
            case DAMAGED:
                return inv.getDamagedQuantity() != null ? inv.getDamagedQuantity() : 0;
            default:
                throw new IllegalArgumentException("不支持的库存桶: " + bucket);
        }
    }

    private void setBucketQuantity(Inventory inv, StockBucket bucket, int quantity) {
        switch (bucket) {
            case AVAILABLE:
                inv.setAvailableQuantity(quantity);
                break;
            case RESERVED:
                inv.setReservedQuantity(quantity);
                break;
            case IN_TRANSIT:
                inv.setInTransitQuantity(quantity);
                break;
            case DAMAGED:
                inv.setDamagedQuantity(quantity);
                break;
            default:
                throw new IllegalArgumentException("不支持的库存桶: " + bucket);
        }
    }

    private void validateConstraint(StockBucket bucket, int afterQuantity, String skuCode) {
        if (!bucket.isAllowNegative() && afterQuantity < 0) {
            throw new IllegalStateException(String.format(
                    "%s库存不足，SKU: %s，变更后数量: %d",
                    bucket.getDescription(), skuCode, afterQuantity));
        }
    }

    /**
     * 构建仓库级流水
     */
    private StockFlow buildWarehouseFlow(StockPosting posting, StockPostingItem item,
                                         int beforeQuantity, int afterQuantity) {
        StockFlow flow = new StockFlow();
        flow.setWarehouseId(item.getWarehouseId());
        flow.setRegionId(0L);  // 仓库级流水，regionId=0
        flow.setErpTenantId(item.getErpTenantId());
        flow.setWmsTenantId(item.getWmsTenantId());
        flow.setSkuCode(item.getSkuCode());
        flow.setBucket(item.getBucket());
        flow.setDirection(item.getDirection());
        flow.setQuantity(item.getQuantity());
        flow.setBeforeQuantity(beforeQuantity);
        flow.setAfterQuantity(afterQuantity);
        flow.setPostingId(posting.getId());
        flow.setPostingItemId(item.getId());
        flow.setPostingNo(posting.getPostingNo());
        flow.setPostingType(posting.getPostingType());
        flow.setSourceType(posting.getSourceType());
        flow.setSourceId(posting.getSourceId());
        flow.setSourceNo(posting.getSourceNo());
        return flow;
    }

    /**
     * 构建区域级流水
     */
    private StockFlow buildRegionFlow(StockPosting posting, StockPostingItem item,
                                      int beforeQuantity, int afterQuantity) {
        StockFlow flow = new StockFlow();
        flow.setWarehouseId(0L);  // 区域级流水，warehouseId=0
        flow.setRegionId(item.getRegionId());
        flow.setErpTenantId(item.getErpTenantId());
        flow.setWmsTenantId(item.getWmsTenantId());
        flow.setSkuCode(item.getSkuCode());
        flow.setBucket(item.getBucket());
        flow.setDirection(item.getDirection());
        flow.setQuantity(item.getQuantity());
        flow.setBeforeQuantity(beforeQuantity);
        flow.setAfterQuantity(afterQuantity);
        flow.setPostingId(posting.getId());
        flow.setPostingItemId(item.getId());
        flow.setPostingNo(posting.getPostingNo());
        flow.setPostingType(posting.getPostingType());
        flow.setSourceType(posting.getSourceType());
        flow.setSourceId(posting.getSourceId());
        flow.setSourceNo(posting.getSourceNo());
        return flow;
    }

}
