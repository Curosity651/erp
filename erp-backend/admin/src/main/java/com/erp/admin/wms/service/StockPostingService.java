package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.StockPostingMapper;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.StockPosting;
import com.erp.admin.wms.model.entity.StockPostingItem;
import com.erp.admin.wms.model.result.StockPostingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 库存过账服务
 * 统一入口，业务层构建完整 DTO，此服务负责持久化和调用 Engine
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPostingService extends ExtendServiceImpl<StockPostingMapper, StockPosting> {

    private static final DateTimeFormatter POSTING_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_POSTING_NO_RETRY = 3;

    private final StockPostingItemService stockPostingItemService;
    private final InventoryPostingEngine inventoryPostingEngine;

    /**
     * 执行库存过账（统一入口）
     *
     * @param dto 过账请求（业务层已构建完整的 items）
     * @return 过账结果
     */
    @Transactional(rollbackFor = Exception.class)
    public StockPostingResult post(StockPostingDTO dto) {
		List<StockPostingItemDTO> mergedItems = mergeItems(dto.getItems());
		boolean aggregateTenantPosting = resolvePostingTenant(dto, mergedItems);

        // 1. 幂等校验（M-2：按货主收窄，防跨租户 source_id 撞号误命中他人过账）
        StockPosting existing = baseMapper.selectBySourceAndType(
                dto.getSourceType(),
                dto.getSourceId(),
                dto.getPostingType().name(),
                dto.getErpTenantId()
        );
        if (existing != null) {
            log.info("幂等命中，sourceType={}, sourceId={}, postingType={}, erpTenantId={}",
                    dto.getSourceType(), dto.getSourceId(), dto.getPostingType(), dto.getErpTenantId());
            return StockPostingResult.builder()
                    .postingId(existing.getId())
                    .postingNo(existing.getPostingNo())
                    .existed(true)
                    .build();
        }

        // 3. 创建过账单（M-2：并发下"先查后插"漏网时，靠 DB 唯一键 uk_source_posting_tenant 兜底；
        //    命中唯一冲突说明他人已并发插入同一来源过账 → 回查返回幂等，不重复应用库存）
        StockPosting posting;
        try {
            posting = createPosting(dto, aggregateTenantPosting);
        } catch (DuplicateKeyException e) {
            StockPosting concurrent = baseMapper.selectBySourceAndType(
                    dto.getSourceType(), dto.getSourceId(), dto.getPostingType().name(), dto.getErpTenantId());
            if (concurrent != null) {
                log.info("过账并发幂等命中(唯一键兜底)，sourceType={}, sourceId={}, postingType={}",
                        dto.getSourceType(), dto.getSourceId(), dto.getPostingType());
                return StockPostingResult.builder()
                        .postingId(concurrent.getId())
                        .postingNo(concurrent.getPostingNo())
                        .existed(true)
                        .build();
            }
            throw e;
        }

        // 4. 创建过账明细
        List<StockPostingItem> items = mergedItems.stream()
                .map(itemDto -> {
                    StockPostingItem item = new StockPostingItem();
                    item.setPostingId(posting.getId());
                    item.setWarehouseId(itemDto.getWarehouseId() != null ? itemDto.getWarehouseId() : 0L);
                    item.setRegionId(itemDto.getRegionId() != null ? itemDto.getRegionId() : 0L);
                    item.setErpTenantId(itemDto.getErpTenantId());
                    item.setWmsTenantId(itemDto.getWmsTenantId());
                    item.setSkuCode(itemDto.getSkuCode());
                    item.setBucket(itemDto.getBucket().name());
                    item.setDirection(itemDto.getDirection().name());
                    item.setQuantity(itemDto.getQuantity());
                    item.setRemark(itemDto.getRemark());
                    return item;
                })
                .collect(Collectors.toList());
        stockPostingItemService.saveBatch(items);

        // 5. 执行库存变更
        inventoryPostingEngine.execute(posting, items);

        // 6. 更新过账时间
        posting.setPostTime(LocalDateTime.now());
        updateById(posting);

        log.info("过账完成，postingNo={}, postingType={}, itemCount={}",
                posting.getPostingNo(), dto.getPostingType(), items.size());

        return StockPostingResult.builder()
                .postingId(posting.getId())
                .postingNo(posting.getPostingNo())
                .existed(false)
                .build();
    }

    /**
     * 检查指定来源和过账类型的过账记录是否存在
     */
    public boolean existsBySourceAndType(String sourceType, Long sourceId, String postingType) {
        return baseMapper.selectBySourceAndType(sourceType, sourceId, postingType) != null;
    }

    /**
     * 根据来源单据查询过账单列表
     *
     * @param sourceType 来源单据类型
     * @param sourceId   来源单据ID
     * @return 过账单列表
     */
    public List<StockPosting> getBySource(String sourceType, Long sourceId) {
        return baseMapper.selectBySource(sourceType, sourceId);
    }

    /**
     * 根据过账单号查询
     *
     * @param postingNo 过账单号
     * @return 过账单
     */
    public StockPosting getByPostingNo(String postingNo) {
        return baseMapper.selectByPostingNo(postingNo);
    }

    /**
     * 生成过账单号
     * 格式: SP + yyyyMMddHHmmss + 4位随机数
     */
    private String generatePostingNo() {
        String timestamp = LocalDateTime.now().format(POSTING_NO_FORMATTER);
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return "SP" + timestamp + random;
    }

    /**
     * 合并相同维度的过账明细
     * 合并维度：(warehouseId, regionId, skuCode, bucket, direction)
     * 防御性校验：同一 (warehouseId, regionId, skuCode, bucket) 不允许同时存在 IN 和 OUT
     */
    private List<StockPostingItemDTO> mergeItems(List<StockPostingItemDTO> items) {
        if (items == null || items.size() <= 1) {
            return items;
        }

        // 按 (warehouseId, regionId, skuCode, bucket, direction) 分组合并
        Map<String, StockPostingItemDTO> mergedMap = new LinkedHashMap<>();
        for (StockPostingItemDTO item : items) {
            String key = buildMergeKey(item);
            mergedMap.compute(key, (k, existing) -> {
                if (existing == null) {
                    return item;
                }
                // 合并数量和备注
                String mergedRemark = mergeRemark(existing.getRemark(), item.getRemark());
                return existing.toBuilder()
                        .quantity(existing.getQuantity() + item.getQuantity())
                        .remark(mergedRemark)
                        .build();
            });
        }

        List<StockPostingItemDTO> result = new ArrayList<>(mergedMap.values());

        // 防御性校验：检查同一 (warehouseId, regionId, skuCode, bucket) 是否同时存在 IN 和 OUT
        validateNoConflictingDirections(result);

        return result;
    }

    /**
     * 构建合并 Key
     */
    public static String buildMergeKey(StockPostingItemDTO item) {
        Long warehouseId = item.getWarehouseId() != null ? item.getWarehouseId() : 0L;
        Long regionId = item.getRegionId() != null ? item.getRegionId() : 0L;
        // 货主维必须进 key：不同货主的同名 SKU 是不同库存主体，绝不能合并
        return item.getErpTenantId() + ":" + item.getWmsTenantId() + ":"
                + warehouseId + ":" + regionId + ":" + item.getSkuCode() + ":"
                + item.getBucket().name() + ":" + item.getDirection().name();
    }

	/**
	 * 校验过账明细货主并确定单头货主。
	 *
	 * @return true 表示明细属于多个货主，单头允许保持空值
	 */
	static boolean resolvePostingTenant(StockPostingDTO dto, List<StockPostingItemDTO> items) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("过账明细不能为空");
		}

		Set<Long> ownerIds = items.stream().map(StockPostingItemDTO::getErpTenantId).collect(Collectors.toSet());
		if (ownerIds.contains(null) || ownerIds.stream().anyMatch(ownerId -> ownerId <= 0)) {
			throw new IllegalArgumentException("过账明细必须指定有效货主");
		}

		Long headerOwnerId = dto.getErpTenantId();
		if (headerOwnerId != null) {
			if (headerOwnerId <= 0) {
				throw new IllegalArgumentException("过账单头货主必须为正数");
			}
			if (ownerIds.size() != 1 || !ownerIds.contains(headerOwnerId)) {
				throw new IllegalArgumentException("过账单头货主与明细货主不一致");
			}
			return false;
		}

		if (ownerIds.size() == 1) {
			dto.setErpTenantId(ownerIds.iterator().next());
			return false;
		}
		return true;
	}

    /**
     * 合并备注，用分号分隔，截断至 200 字符
     */
    private String mergeRemark(String remark1, String remark2) {
        if (StrUtil.isBlank(remark1) && StrUtil.isBlank(remark2)) {
            return null;
        }
        if (StrUtil.isBlank(remark1)) {
            return remark2;
        }
        if (StrUtil.isBlank(remark2)) {
            return remark1;
        }
        String merged = remark1 + "; " + remark2;
        return merged.length() > 200 ? merged.substring(0, 200) : merged;
    }

    /**
     * 校验同一 (warehouseId, regionId, skuCode, bucket) 不允许同时存在 IN 和 OUT
     */
    private void validateNoConflictingDirections(List<StockPostingItemDTO> items) {
        // 按 (warehouseId, regionId, skuCode, bucket) 分组，检查是否有多个 direction
        Map<String, List<StockPostingItemDTO>> groupByBucket = items.stream()
                .collect(Collectors.groupingBy(item -> {
                    Long warehouseId = item.getWarehouseId() != null ? item.getWarehouseId() : 0L;
                    Long regionId = item.getRegionId() != null ? item.getRegionId() : 0L;
                    return item.getErpTenantId() + ":" + item.getWmsTenantId() + ":"
                            + warehouseId + ":" + regionId + ":" + item.getSkuCode() + ":" + item.getBucket().name();
                }));

        for (Map.Entry<String, List<StockPostingItemDTO>> entry : groupByBucket.entrySet()) {
            List<StockPostingItemDTO> group = entry.getValue();
            if (group.size() > 1) {
                // 同一 bucket 出现多条记录，说明存在不同的 direction
                throw new IllegalArgumentException(
                        "过账明细异常：同一库存位置不允许同时存在 IN 和 OUT 操作，key=" + entry.getKey());
            }
        }
    }

    /**
     * 创建过账单（带单号生成重试机制）
     */
    private StockPosting createPosting(StockPostingDTO dto, boolean aggregateTenantPosting) {
        for (int i = 0; i < MAX_POSTING_NO_RETRY; i++) {
            try {
                StockPosting posting = new StockPosting();
                posting.setPostingNo(generatePostingNo());
                posting.setWarehouseId(dto.getWarehouseId() != null ? dto.getWarehouseId() : 0L);
                posting.setRegionId(dto.getRegionId() != null ? dto.getRegionId() : 0L);
                posting.setErpTenantId(dto.getErpTenantId());
				posting.setAggregateTenantPosting(aggregateTenantPosting);
                posting.setWmsTenantId(dto.getWmsTenantId());
                posting.setPostingType(dto.getPostingType().name());
                posting.setSourceType(dto.getSourceType());
                posting.setSourceId(dto.getSourceId());
                posting.setSourceNo(dto.getSourceNo());
                posting.setBizTime(dto.getBizTime() != null ? dto.getBizTime() : LocalDateTime.now());
                posting.setRemark(dto.getRemark());
                save(posting);
                return posting;
            } catch (DuplicateKeyException e) {
                String message = e.getMessage();
                if (message != null && message.contains("posting_no")) {
                    if (i == MAX_POSTING_NO_RETRY - 1) {
                        log.error("过账单号生成失败，已重试{}次", MAX_POSTING_NO_RETRY);
                        throw new RuntimeException("过账单号生成失败，请重试");
                    }
                    log.warn("过账单号重复，重试第{}次", i + 1);
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("过账单号生成失败");
    }

}
