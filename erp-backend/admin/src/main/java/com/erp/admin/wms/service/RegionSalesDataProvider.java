package com.erp.admin.wms.service;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.wms.constant.ForecastConstants;
import com.erp.admin.wms.mapper.PlatformRegionMappingMapper;
import com.erp.admin.wms.model.dto.PlatformSkuSalesDTO;
import com.erp.admin.wms.model.entity.PlatformRegionMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 区域销量数据提供者
 * <p>
 * 按区域统计 FBS 订单日均销量。
 * <p>
 * 算法: 加权移动平均 + 有效天数校正
 * - 近 7 天权重 = 2，8~30 天权重 = 1
 * - 有效天数 = min(30, SKU 首次出单至今天数)
 * - dailySales = ceil(加权销量 / 加权天数)
 */
@Service
@RequiredArgsConstructor
public class RegionSalesDataProvider {

    private final ErpOrderMapper erpOrderMapper;
    private final PlatformRegionMappingMapper platformRegionMappingMapper;

    /**
     * 批量查询区域 SKU 日均销量（加权移动平均）
     *
     * @param regionIds 区域 ID 集合
     * @param skuCodes  SKU 编码集合
     * @return Map<"regionId:skuCode", dailySales>
     */
    public Map<String, Integer> getRegionDailySalesMap(Set<Long> regionIds, Set<String> skuCodes) {
        if (regionIds.isEmpty() || skuCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        // Step 1: 查询区域 → 平台映射
        Map<Long, List<String>> regionPlatformsMap = getRegionPlatformsMap(regionIds);
        if (regionPlatformsMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // Step 2: 收集所有关联的平台
        Set<String> allPlatforms = regionPlatformsMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (allPlatforms.isEmpty()) {
            return Collections.emptyMap();
        }

        // Step 3: 分段查询销量
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recent7Start = now.minusDays(ForecastConstants.RECENT_DAYS);
        LocalDateTime older30Start = now.minusDays(ForecastConstants.SALES_STAT_DAYS);

        // 近 7 天销量
        List<PlatformSkuSalesDTO> recentSalesList =
                erpOrderMapper.selectFbsRecentSales(allPlatforms, skuCodes, recent7Start, now);
        Map<String, Integer> recentSalesMap = toSalesMap(recentSalesList);

        // 8~30 天销量
        List<PlatformSkuSalesDTO> olderSalesList =
                erpOrderMapper.selectFbsRecentSales(allPlatforms, skuCodes, older30Start, recent7Start);
        Map<String, Integer> olderSalesMap = toSalesMap(olderSalesList);

        // Step 4: 查询首次出单时间
        List<PlatformSkuSalesDTO> firstOrderList =
                erpOrderMapper.selectFirstFbsOrderTime(allPlatforms, skuCodes);
        Map<String, LocalDateTime> firstOrderMap = firstOrderList.stream()
                .filter(dto -> dto.getFirstOrderTime() != null)
                .collect(Collectors.toMap(
                        dto -> dto.getPlatform() + ":" + dto.getSkuCode(),
                        PlatformSkuSalesDTO::getFirstOrderTime,
                        (a, b) -> a.isBefore(b) ? a : b
                ));

        // Step 5: 按区域聚合并计算加权日均
        Map<String, Integer> result = new HashMap<>();
        for (Long regionId : regionIds) {
            List<String> platforms = regionPlatformsMap.getOrDefault(regionId, Collections.emptyList());
            for (String skuCode : skuCodes) {
                int recentSales = 0;
                int olderSales = 0;
                LocalDateTime earliestOrder = null;

                for (String platform : platforms) {
                    String pKey = platform + ":" + skuCode;
                    recentSales += recentSalesMap.getOrDefault(pKey, 0);
                    olderSales += olderSalesMap.getOrDefault(pKey, 0);
                    LocalDateTime first = firstOrderMap.get(pKey);
                    if (first != null && (earliestOrder == null || first.isBefore(earliestOrder))) {
                        earliestOrder = first;
                    }
                }

                int totalSales = recentSales + olderSales;
                if (totalSales <= 0) {
                    continue;
                }

                // 有效天数 = min(30, 首次出单至今天数)
                int effectiveDays;
                if (earliestOrder != null) {
                    long daysSinceFirst = ChronoUnit.DAYS.between(
                            earliestOrder.toLocalDate(), LocalDate.now());
                    effectiveDays = (int) Math.min(
                            ForecastConstants.SALES_STAT_DAYS, Math.max(1, daysSinceFirst));
                } else {
                    effectiveDays = ForecastConstants.SALES_STAT_DAYS;
                }

                // 加权计算
                int dailySales = calculateWeightedDailySales(recentSales, olderSales, effectiveDays);
                if (dailySales > 0) {
                    result.put(regionId + ":" + skuCode, dailySales);
                }
            }
        }
        return result;
    }

    /**
     * 查询单个区域单个 SKU 的日均销量
     */
    public int getRegionDailySales(Long regionId, String skuCode) {
        Map<String, Integer> map = getRegionDailySalesMap(
                Collections.singleton(regionId),
                Collections.singleton(skuCode));
        return map.getOrDefault(regionId + ":" + skuCode, 0);
    }

    /**
     * 加权移动平均日均销量
     * <p>
     * 公式: ceil((recentSales × Wr + olderSales × Wo) / (min(7, D) × Wr + max(0, D-7) × Wo))
     *
     * @param recentSales   近 7 天销量
     * @param olderSales    8~30 天销量
     * @param effectiveDays 有效销售天数 (1~30)
     * @return 加权日均销量（天花板取整，≥1）
     */
    static int calculateWeightedDailySales(int recentSales, int olderSales, int effectiveDays) {
        int recentDays = Math.min(ForecastConstants.RECENT_DAYS, effectiveDays);
        int olderDays = Math.max(0, effectiveDays - ForecastConstants.RECENT_DAYS);

        int weightedSales = recentSales * ForecastConstants.RECENT_WEIGHT
                + olderSales * ForecastConstants.OLDER_WEIGHT;
        int weightedDays = recentDays * ForecastConstants.RECENT_WEIGHT
                + olderDays * ForecastConstants.OLDER_WEIGHT;

        if (weightedDays <= 0) {
            return 0;
        }

        // 天花板取整: (a + b - 1) / b
        return (weightedSales + weightedDays - 1) / weightedDays;
    }

    // ========== 内部方法 ==========

    private Map<String, Integer> toSalesMap(List<PlatformSkuSalesDTO> list) {
        return list.stream().collect(Collectors.toMap(
                dto -> dto.getPlatform() + ":" + dto.getSkuCode(),
                PlatformSkuSalesDTO::getTotalQuantity,
                Integer::sum
        ));
    }

    private Map<Long, List<String>> getRegionPlatformsMap(Set<Long> regionIds) {
        List<PlatformRegionMapping> mappings = platformRegionMappingMapper
                .selectByRegionIds(new ArrayList<>(regionIds));
        return mappings.stream().collect(Collectors.groupingBy(
                PlatformRegionMapping::getRegionId,
                Collectors.mapping(PlatformRegionMapping::getPlatform, Collectors.toList())));
    }
}
