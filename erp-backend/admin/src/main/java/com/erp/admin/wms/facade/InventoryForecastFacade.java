package com.erp.admin.wms.facade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.constant.ForecastConstants;
import com.erp.admin.wms.model.dto.RegionSkuStockDTO;
import com.erp.admin.wms.model.dto.EffectiveInventoryConfig;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.entity.InventoryConfig;
import com.erp.admin.wms.model.enums.ForecastStatus;
import com.erp.admin.wms.model.qo.ForecastDetailQO;
import com.erp.admin.wms.model.qo.ForecastSummaryQO;
import com.erp.admin.wms.model.vo.ForecastDetailVO;
import com.erp.admin.wms.model.vo.ForecastDetailVO.CurrentStockVO;
import com.erp.admin.wms.model.vo.ForecastDetailVO.ForecastDayVO;
import com.erp.admin.wms.model.vo.ForecastDetailVO.IncomingDetailVO;
import com.erp.admin.wms.model.vo.ForecastDetailVO.IncomingSourceVO;
import com.erp.admin.wms.model.vo.ForecastSummaryResult;
import com.erp.admin.wms.model.vo.ForecastSummaryVO;
import com.erp.admin.wms.model.vo.IncomingPlanVO;
import com.erp.admin.wms.service.InventoryConfigService;
import com.erp.admin.wms.service.RegionSalesDataProvider;
import com.erp.admin.wms.service.RegionService;
import com.erp.admin.wms.service.RegionStockDataProvider;
import com.erp.admin.wms.service.ShippingOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.springframework.stereotype.Component;

/**
 * 库存预测 Facade
 * <p>
 * 业务编排层，组合多个 Service 完成库存预测功能。
 * 本类不对应具体数据库表，仅负责跨服务的数据聚合与业务编排。
 * <p>
 * 迭代4改造：从「仓库×SKU」维度改为「区域×SKU」维度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryForecastFacade {

    private final RegionService regionService;
    private final InventoryConfigService inventoryConfigService;
    private final ShippingOrderService shippingOrderService;
    private final SkuBriefService skuBriefService;
    private final RegionStockDataProvider regionStockDataProvider;
    private final RegionSalesDataProvider regionSalesDataProvider;

    /**
     * 获取库存预测汇总（区域维度）
     */
    public ForecastSummaryResult getSummary(PageParam pageParam, ForecastSummaryQO qo) {
        // 获取全局预警阈值
        int thresholdDays = inventoryConfigService.getGlobalThresholdDays();
        int forecastDays = qo.getDays() != null ? qo.getDays() : ForecastConstants.DEFAULT_FORECAST_DAYS;

        // 1. 查询区域列表
        Set<Long> regionIds;
        Map<Long, String> regionNameMap;
        if (qo.getRegionId() != null) {
            // 指定区域
            regionIds = Collections.singleton(qo.getRegionId());
            regionNameMap = regionService.getNameMapByIds(regionIds);
        } else {
            // 所有启用区域
            List<Region> regions = regionService.listEnabled();
            if (regions.isEmpty()) {
                return ForecastSummaryResult.empty(thresholdDays);
            }
            regionIds = regions.stream().map(Region::getId).collect(Collectors.toSet());
            regionNameMap = regions.stream()
                    .collect(Collectors.toMap(Region::getId, Region::getRegionName));
        }

        // 2. 查询区域 SKU 库存聚合
        List<RegionSkuStockDTO> stockList = new ArrayList<>(regionStockDataProvider.getRegionSkuStocks(
                regionIds, qo.getSkuKeyword()));
		Map<String, RegionSkuStockDTO> stockByKey = stockList.stream().collect(Collectors.toMap(
				row -> row.getRegionId() + ":" + row.getSkuCode(), row -> row, (a, b) -> a));
		Map<Long, Set<String>> salesSkus = regionSalesDataProvider.getRegionSkuCodesWithSales(regionIds,
				qo.getSkuKeyword());
		for (Map.Entry<Long, Set<String>> entry : salesSkus.entrySet()) {
			for (String sku : entry.getValue()) {
				stockByKey.putIfAbsent(entry.getKey() + ":" + sku, RegionSkuStockDTO.empty(entry.getKey(), sku));
			}
		}
		for (InventoryConfig config : inventoryConfigService.listConfiguredRegionSkus(regionIds, qo.getSkuKeyword())) {
			stockByKey.putIfAbsent(config.getRegionId() + ":" + config.getSkuCode(),
					RegionSkuStockDTO.empty(config.getRegionId(), config.getSkuCode()));
		}
		stockList = new ArrayList<>(stockByKey.values());
		if (stockList.isEmpty()) return ForecastSummaryResult.empty(thresholdDays);

        // 3. 收集 SKU 编码
        Set<String> skuCodes = stockList.stream()
                .map(RegionSkuStockDTO::getSkuCode)
                .collect(Collectors.toSet());

        // 4. 批量查询区域预占
        Map<String, Integer> reservedMap = regionStockDataProvider.getRegionReservedMap(regionIds, skuCodes);

        // 5. 批量查询待发货库存（按区域聚合）
        Map<String, Integer> pendingShipmentMap = getPendingShipmentByRegion(regionIds, skuCodes);

        // 5.5 批量查询入库计划（按区域+日期聚合）
        Map<String, Map<LocalDate, Integer>> incomingByDateMap =
                batchGetRegionIncomingByDate(regionIds, skuCodes);

        // 6. 批量查询日均销量（区域维度）
        Map<String, Integer> dailySalesMap = regionSalesDataProvider.getRegionDailySalesMap(regionIds, skuCodes);

        // 7. 构建预测结果
        List<ForecastSummaryVO> allResults = buildRegionForecastSummaryList(
                stockList, regionNameMap, reservedMap, pendingShipmentMap, dailySalesMap,
                incomingByDateMap, thresholdDays, forecastDays);

        // 8. 统计状态数量
        Map<ForecastStatus, Integer> statusCounts = countByStatus(allResults);

        // 9. 排序（按可售天数升序）
        sortBySellableDays(allResults);

        // 10. 筛选状态
        List<ForecastSummaryVO> filteredResults = filterByStatus(allResults, qo.getStatus());

        // 11. 分页
        List<ForecastSummaryVO> pagedResults = applyPagination(filteredResults, pageParam);

        // 12. 填充 SKU 简要信息
        enrichSkuBrief(pagedResults);

        return new ForecastSummaryResult(filteredResults.size(), statusCounts, thresholdDays, pagedResults);
    }

    /**
     * 获取单 SKU 预测详情（区域维度）
     */
    public ForecastDetailVO getDetail(ForecastDetailQO qo) {
        Long regionId = qo.getRegionId();
        String skuCode = qo.getSkuCode();

        // 获取配置
        EffectiveInventoryConfig effectiveConfig = inventoryConfigService.getEffectiveConfig(regionId, skuCode);
        int thresholdDays = effectiveConfig.getNotifyThresholdDays();
        int forecastDays = qo.getDays() != null ? qo.getDays() : ForecastConstants.DEFAULT_FORECAST_DAYS;

        // 1. 查询当前库存（区域聚合）
        CurrentStockVO currentStock = buildRegionCurrentStock(regionId, skuCode);

        // 2. 获取日均销量（支持临时覆盖）
        int dailySales = getRegionDailySales(qo);

        // 2.5 计算动态安全库存（与汇总页一致，支持 per-region/SKU 配置覆盖）
        int safetyStock = inventoryConfigService.getEffectiveSafetyStock(regionId, skuCode);
        int effectiveSafety = inventoryConfigService.getEffectiveSafetyStockDynamic(regionId, skuCode, dailySales);

        // 3. 获取入库计划（按区域直接查询，只查一次复用）
        List<IncomingPlanVO> incomingPlans = shippingOrderService.getIncomingPlan(regionId, skuCode);
        Map<LocalDate, List<IncomingDetailVO>> incomingByDate =
                buildIncomingDetailsByDate(incomingPlans);

        // 4. 生成每日预测列表
        List<ForecastDayVO> forecastList = generateDailyForecast(
                currentStock.getSellable(), dailySales, incomingByDate, forecastDays, effectiveSafety);

        // 5. 计算可售天数
        Integer sellableDays = calculateSellableDaysFromForecast(forecastList, dailySales);

        // 6. 获取入库来源明细（复用入库计划数据）
        List<IncomingSourceVO> incomingSources = buildIncomingSources(incomingPlans, skuCode);

        // 7. 构建结果
        ForecastDetailVO result = new ForecastDetailVO();
        result.setRegionId(regionId);
        result.setRegionName(regionService.getNameMapByIds(Collections.singleton(regionId)).get(regionId));
        result.setCurrentStock(currentStock);
        result.setSafetyStock(safetyStock);
        result.setEffectiveSafetyStock(effectiveSafety);
        result.setDailySales(dailySales);
        result.setSellableDays(sellableDays);
        result.setStatus(ForecastStatus.fromSellableDays(sellableDays, thresholdDays).name());
        result.setThresholdDays(thresholdDays);
		result.setNotifyEnabled(effectiveConfig.getNotifyEnabled());
		result.setNotifyThresholdDays(effectiveConfig.getNotifyThresholdDays());
		result.setConfigSource(effectiveConfig.getSource());
        result.setForecastList(forecastList);
        result.setIncomingSources(incomingSources);
        result.setPendingShipmentTotal(currentStock.getPendingShipment());

        // 8. 填充 SKU 简要信息
        Map<String, SkuBriefVO> skuBriefMap =
                skuBriefService.buildMapForQuery(Collections.singletonList(skuCode));
        result.setSkuBrief(skuBriefMap.get(skuCode));

        return result;
    }

    /**
     * 获取入库计划（区域级）
     */
    public List<IncomingPlanVO> getIncomingPlan(Long regionId, String skuCode) {
        return shippingOrderService.getIncomingPlan(regionId, skuCode);
    }

    // ========== 私有方法：汇总页相关 ==========

    /**
     * 构建区域预测汇总列表
     */
    private List<ForecastSummaryVO> buildRegionForecastSummaryList(
            List<RegionSkuStockDTO> stockList,
            Map<Long, String> regionNameMap,
            Map<String, Integer> reservedMap,
            Map<String, Integer> pendingShipmentMap,
            Map<String, Integer> dailySalesMap,
            Map<String, Map<LocalDate, Integer>> incomingByDateMap,
            int thresholdDays,
            int forecastDays) {

        List<ForecastSummaryVO> results = new ArrayList<>();
        for (RegionSkuStockDTO stock : stockList) {
            String key = stock.getRegionId() + ":" + stock.getSkuCode();

            ForecastSummaryVO vo = new ForecastSummaryVO();
            vo.setSkuCode(stock.getSkuCode());
            vo.setRegionId(stock.getRegionId());
            vo.setRegionName(regionNameMap.get(stock.getRegionId()));
            vo.setAvailableQuantity(stock.getTotalAvailable());
            vo.setInTransitQuantity(stock.getTotalInTransit());

            // 预占和可售
            int reserved = reservedMap.getOrDefault(key, 0);
            vo.setReservedQuantity(reserved);
            // 新库存查询层返回的 available 已经扣除预占，禁止二次扣减。
            vo.setSellableQuantity(stock.getTotalAvailable());

            vo.setPendingShipmentQuantity(pendingShipmentMap.getOrDefault(key, 0));

            // 计算动态安全库存
            int dailySales = dailySalesMap.getOrDefault(key, 0);
            EffectiveInventoryConfig effectiveConfig = inventoryConfigService.getEffectiveConfig(
                    stock.getRegionId(), stock.getSkuCode());
            int effectiveSafety = inventoryConfigService.getEffectiveSafetyStockDynamic(
                    stock.getRegionId(), stock.getSkuCode(), dailySales);

            vo.setDailySales(dailySales);

            // 计算可售天数（逐日模拟，统一算法）
            Map<LocalDate, Integer> incomingByDate = incomingByDateMap.getOrDefault(key, Collections.emptyMap());
            calculateRegionSellableDays(vo, incomingByDate, effectiveSafety,
                    effectiveConfig.getNotifyThresholdDays(), forecastDays);

            results.add(vo);
        }
        return results;
    }

    /**
     * 计算区域可售天数（逐日模拟）
     */
    private void calculateRegionSellableDays(ForecastSummaryVO vo,
                                              Map<LocalDate, Integer> incomingByDate,
                                              int effectiveSafety,
                                              int thresholdDays, int forecastDays) {
        int sellable = vo.getSellableQuantity();
        int dailySales = vo.getDailySales();

        // 已断货
        if (sellable <= 0) {
            vo.setSellableDays(0);
            vo.setStatus(ForecastStatus.STOCKOUT);
            vo.setStockoutDate(LocalDate.now());
            vo.setEffectiveSafetyStock(effectiveSafety);
            return;
        }

        // 无销量数据
        if (dailySales <= 0) {
            vo.setSellableDays(null);
            vo.setStatus(ForecastStatus.NO_SALES);
            vo.setStockoutDate(null);
            vo.setEffectiveSafetyStock(effectiveSafety);
            return;
        }

        SimulationResult sim = simulate(sellable, dailySales, incomingByDate, forecastDays);

        vo.setSellableDays(sim.sellableDays);
        vo.setEffectiveSafetyStock(effectiveSafety);
        vo.setStatus(ForecastStatus.fromSellableDays(sim.sellableDays, thresholdDays));
        vo.setStockoutDate(sim.sellableDays != null && sim.sellableDays < forecastDays
                ? LocalDate.now().plusDays(sim.sellableDays) : null);
    }

    /**
     * 按区域查询待发货库存（直接按区域查询，不再通过仓库中转）
     */
    private Map<String, Integer> getPendingShipmentByRegion(Set<Long> regionIds, Set<String> skuCodes) {
        return shippingOrderService.getPendingShipmentQuantityMap(regionIds, skuCodes);
    }

    /**
     * 统计各状态数量
     */
    private Map<ForecastStatus, Integer> countByStatus(List<ForecastSummaryVO> results) {
        Map<ForecastStatus, Integer> statusCounts = new EnumMap<>(ForecastStatus.class);
        Arrays.stream(ForecastStatus.values()).forEach(s -> statusCounts.put(s, 0));
        for (ForecastSummaryVO vo : results) {
            statusCounts.merge(vo.getStatus(), 1, Integer::sum);
        }
        return statusCounts;
    }

    /**
     * 按可售天数升序排序（null 视为最大）
     */
    private void sortBySellableDays(List<ForecastSummaryVO> results) {
        results.sort((a, b) -> {
            if (a.getSellableDays() == null && b.getSellableDays() == null) return 0;
            if (a.getSellableDays() == null) return 1;
            if (b.getSellableDays() == null) return -1;
            return a.getSellableDays().compareTo(b.getSellableDays());
        });
    }

    /**
     * 按状态筛选
     */
    private List<ForecastSummaryVO> filterByStatus(List<ForecastSummaryVO> results, ForecastStatus status) {
        if (status == null) {
            return results;
        }
        return results.stream()
                .filter(v -> v.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 应用分页
     */
    private List<ForecastSummaryVO> applyPagination(List<ForecastSummaryVO> results, PageParam pageParam) {
        int total = results.size();
        int start = (int) ((pageParam.getPage() - 1) * pageParam.getSize());
        int end = Math.min(start + (int) pageParam.getSize(), total);
        return start < total ? results.subList(start, end) : Collections.emptyList();
    }

    /**
     * 填充 SKU 简要信息
     */
    private void enrichSkuBrief(List<ForecastSummaryVO> results) {
        if (!results.isEmpty()) {
            skuBriefService.enrichForQuery(results,
                    ForecastSummaryVO::getSkuCode,
                    ForecastSummaryVO::setSkuBrief);
        }
    }

    // ========== 私有方法：详情页相关 ==========

    /**
     * 构建区域当前库存
     */
    private CurrentStockVO buildRegionCurrentStock(Long regionId, String skuCode) {
        RegionSkuStockDTO stock = regionStockDataProvider.getRegionSkuStock(regionId, skuCode);
        int reserved = regionStockDataProvider.getRegionReserved(regionId, skuCode);

        // 查询待发货
        Map<String, Integer> pendingMap = getPendingShipmentByRegion(
                Collections.singleton(regionId), Collections.singleton(skuCode));
        int pending = pendingMap.getOrDefault(regionId + ":" + skuCode, 0);

        CurrentStockVO currentStock = new CurrentStockVO();
        currentStock.setAvailable(stock.getTotalAvailable());
        currentStock.setReserved(reserved);
        currentStock.setSellable(stock.getTotalAvailable());
        currentStock.setInTransit(stock.getTotalInTransit());
        currentStock.setPendingShipment(pending);

        return currentStock;
    }

    /**
     * 获取区域日均销量（支持临时覆盖）
     */
    private int getRegionDailySales(ForecastDetailQO qo) {
        if (qo.getDailySales() != null && qo.getDailySales() > 0) {
            return qo.getDailySales();
        }
        return regionSalesDataProvider.getRegionDailySales(qo.getRegionId(), qo.getSkuCode());
    }

    /**
     * 将入库计划转为按日期分组的入库明细
     */
    private Map<LocalDate, List<IncomingDetailVO>> buildIncomingDetailsByDate(
            List<IncomingPlanVO> plans) {
        Map<LocalDate, List<IncomingDetailVO>> result = new HashMap<>();
        for (IncomingPlanVO plan : plans) {
            if (plan.getEstimatedArrivalDate() == null) {
                continue;
            }
            IncomingDetailVO detail = new IncomingDetailVO();
            detail.setType("IN_TRANSIT");
            detail.setSourceNo(plan.getShippingNo());
            detail.setQuantity(plan.getQuantity());
            detail.setStatus("运输中");

            result.computeIfAbsent(plan.getEstimatedArrivalDate(), k -> new ArrayList<>()).add(detail);
        }
        return result;
    }

    /**
     * 将入库计划转为入库来源明细
     */
    private List<IncomingSourceVO> buildIncomingSources(List<IncomingPlanVO> plans, String skuCode) {
        List<IncomingSourceVO> result = new ArrayList<>();
        for (IncomingPlanVO plan : plans) {
            IncomingSourceVO source = new IncomingSourceVO();
            source.setType("LOGISTICS");
            source.setSourceNo(plan.getShippingNo());
            source.setSkuCode(skuCode);
            source.setQuantity(plan.getQuantity());
            source.setExpectedDate(plan.getEstimatedArrivalDate());
            result.add(source);
        }
        return result;
    }

    /**
     * 生成每日预测列表
     */
    private List<ForecastDayVO> generateDailyForecast(
            int openingStock,
            int dailySales,
            Map<LocalDate, List<IncomingDetailVO>> incomingByDate,
            int forecastDays,
            int effectiveSafety) {

        List<ForecastDayVO> forecastList = new ArrayList<>();
        int currentStock = openingStock;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < forecastDays; i++) {
            LocalDate date = today.plusDays(i);
            ForecastDayVO day = new ForecastDayVO();
            day.setDate(date);
            day.setOpeningStock(currentStock);

            // 当日入库
            List<IncomingDetailVO> incomingDetails = incomingByDate.getOrDefault(date, Collections.emptyList());
            int incoming = incomingDetails.stream().mapToInt(IncomingDetailVO::getQuantity).sum();
            day.setIncoming(incoming);
            day.setIncomingDetails(incomingDetails);

            // 当日销量
            day.setSales(dailySales);

            // 期末库存
            int closingStock = currentStock + incoming - dailySales;
            day.setClosingStock(closingStock);

            // 状态
            day.setStatus(ForecastStatus.fromStock(closingStock, effectiveSafety).name());

            forecastList.add(day);
            currentStock = closingStock;
        }
        return forecastList;
    }

    /**
     * 从预测列表计算可售天数
     */
    private Integer calculateSellableDaysFromForecast(List<ForecastDayVO> forecastList, int dailySales) {
        if (forecastList.isEmpty()) {
            return 0;
        }

        int openingStock = forecastList.get(0).getOpeningStock();
        if (openingStock <= 0) {
            return 0;
        }
        if (dailySales <= 0) {
            return null;
        }

        for (int i = 0; i < forecastList.size(); i++) {
            if (forecastList.get(i).getClosingStock() <= 0) {
                return i;
            }
        }

        return forecastList.size();
    }

    /**
     * 逐日模拟结果
     */
    private static class SimulationResult {
        /** 可售天数（首次 closingStock ≤ 0） */
        Integer sellableDays;
    }

    /**
     * 统一的逐日模拟
     * <p>
     * 汇总页和详情页共用此逻辑，确保同一 SKU 在两个页面显示一致。
     * 计算「可售天数」— 首次跌破 0（真正断货）的天数。
     *
     * @param sellable       可售库存
     * @param dailySales     日均销量
     * @param incomingByDate 入库计划（日期 → 数量）
     * @param maxDays        最大预测天数
     * @return 模拟结果
     */
    private SimulationResult simulate(int sellable, int dailySales,
                                      Map<LocalDate, Integer> incomingByDate,
                                      int maxDays) {
        SimulationResult result = new SimulationResult();

        if (sellable <= 0) {
            result.sellableDays = 0;
            return result;
        }
        if (dailySales <= 0) {
            result.sellableDays = null;
            return result;
        }

        int stock = sellable;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < maxDays; i++) {
            int incoming = incomingByDate != null
                    ? incomingByDate.getOrDefault(today.plusDays(i), 0) : 0;
            stock = stock + incoming - dailySales;

            // 首次断货（可售天数）
            if (stock <= 0) {
                result.sellableDays = i;
                return result;
            }
        }

        // 预测期内未断货
        result.sellableDays = maxDays;
        return result;
    }

    /**
     * 批量获取区域级入库计划（按日期聚合，直接按区域查询）
     *
     * @param regionIds 区域 ID 集合
     * @param skuCodes  SKU 编码集合
     * @return Map<"regionId:skuCode", Map<日期, 入库量>>
     */
    private Map<String, Map<LocalDate, Integer>> batchGetRegionIncomingByDate(
            Set<Long> regionIds, Set<String> skuCodes) {
        return shippingOrderService.batchGetIncomingByDate(regionIds, skuCodes);
    }
}
