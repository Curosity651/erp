package com.erp.admin.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.admin.statistics.mapper.DashboardMapper;
import com.erp.admin.statistics.model.dto.DashboardQueryDTO;
import com.erp.admin.statistics.model.qo.SkuRankingQO;
import com.erp.admin.statistics.model.stat.ArticleSalesStat;
import com.erp.admin.statistics.model.stat.OrderStatusStat;
import com.erp.admin.statistics.model.stat.PlatformFulfillmentStat;
import com.erp.admin.statistics.model.stat.SalesTrendStat;
import com.erp.admin.statistics.model.vo.DashboardDataVO;
import com.erp.admin.statistics.model.vo.ExchangeRatesVO;
import com.erp.admin.statistics.model.vo.OrderStatusVO;
import com.erp.admin.statistics.model.vo.PlatformFulfillmentVO;
import com.erp.admin.statistics.model.vo.SalesOverviewVO;
import com.erp.admin.statistics.model.vo.SalesTrendVO;
import com.erp.admin.statistics.model.vo.SkuRankingExportVO;
import com.erp.admin.statistics.model.vo.SkuRankingItemVO;
import com.erp.admin.statistics.model.vo.SkuRankingVO;
import com.erp.admin.statistics.model.vo.TargetProgressVO;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.system.mapper.ExchangeRateMapper;
import com.erp.admin.statistics.mapper.SalesTargetMapper;
import com.erp.admin.system.model.entity.ExchangeRate;
import com.erp.admin.statistics.model.SalesTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.stereotype.Service;

/**
 * Dashboard统计服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

	private final DashboardMapper dashboardMapper;

	private final SalesTargetMapper salesTargetMapper;

	private final ExchangeRateMapper exchangeRateMapper;

	private final SkuMappingService skuMappingService;

	/**
	 * 获取Dashboard统计数据（并行查询优化）
	 *
	 * @param queryDTO 查询参数
	 * @return Dashboard数据
	 */
	public DashboardDataVO getDashboardData(DashboardQueryDTO queryDTO) {
		log.info("获取Dashboard数据, 参数: {}", queryDTO);

		// 参数校验
		if (queryDTO.getStartDate() == null || queryDTO.getEndDate() == null) {
			throw new IllegalArgumentException("开始日期和结束日期不能为空");
		}

		if (queryDTO.getStartDate().isAfter(queryDTO.getEndDate())) {
			throw new IllegalArgumentException("开始日期不能晚于结束日期");
		}

		// skuCodes → platformItemIds 预解析
		if (!CollectionUtils.isEmpty(queryDTO.getSkuCodes())) {
			queryDTO.setPlatformItemIds(skuMappingService
					.resolvePlatformItemIdsForQuery(queryDTO.getSkuCodes()));
		}

		long startTime = System.currentTimeMillis();

		// 在请求线程上捕获租户上下文，供并行子线程重建（见 supplyAsyncWithTenant 说明）
		final Long erpTenantId = TenantContext.getCurrentTenant();
		final Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();

		// 并行查询各个统计模块
		CompletableFuture<SalesOverviewVO> salesOverviewFuture = supplyAsyncWithTenant(erpTenantId, wmsTenantId,
			() -> calculateSalesOverview(queryDTO));

		CompletableFuture<TargetProgressVO> targetProgressFuture = supplyAsyncWithTenant(erpTenantId, wmsTenantId,
			() -> calculateTargetProgress(queryDTO.getPlatform()));

		CompletableFuture<ExchangeRatesVO> exchangeRatesFuture = supplyAsyncWithTenant(erpTenantId, wmsTenantId,
			this::getExchangeRates);

		CompletableFuture<List<PlatformFulfillmentVO>> platformFulfillmentFuture = supplyAsyncWithTenant(erpTenantId,
			wmsTenantId, () -> calculatePlatformFulfillment(queryDTO));

		CompletableFuture<SalesTrendVO> salesTrendFuture = supplyAsyncWithTenant(erpTenantId, wmsTenantId,
			() -> calculateSalesTrend(queryDTO));

		CompletableFuture<List<OrderStatusVO>> orderStatusFuture = supplyAsyncWithTenant(erpTenantId, wmsTenantId,
			() -> calculateOrderStatus(queryDTO));

		CompletableFuture<SkuRankingVO> skuRankingFuture = supplyAsyncWithTenant(erpTenantId, wmsTenantId,
			() -> calculateSkuRanking(queryDTO));

		// 等待所有查询完成并组装结果
		try {
			DashboardDataVO result = new DashboardDataVO();
			result.setSalesOverview(salesOverviewFuture.join());
			result.setTargetProgress(targetProgressFuture.join());
			result.setExchangeRates(exchangeRatesFuture.join());
			result.setPlatformFulfillment(platformFulfillmentFuture.join());
			result.setSalesTrend(salesTrendFuture.join());
			result.setOrderStatus(orderStatusFuture.join());
			result.setSkuRanking(skuRankingFuture.join());

			long endTime = System.currentTimeMillis();
			log.info("Dashboard数据查询完成, 耗时: {}ms", endTime - startTime);

			return result;
		}
		catch (Exception e) {
			log.error("Dashboard数据查询失败", e);
			throw new RuntimeException("Dashboard数据查询失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 在异步线程内重建调用线程的租户上下文后执行任务。
	 *
	 * <p>{@link CompletableFuture#supplyAsync} 默认使用 ForkJoinPool.commonPool，线程被复用且
	 * {@link ThreadLocal}（含 {@code InheritableThreadLocal}）不随任务传播 —— 子线程里
	 * {@link TenantContext#getCurrentTenant()} 为 null（或残留上一次任务的租户），导致
	 * {@link ErpTenantLineHandler} 跳过 {@code WHERE tenant_id=?} 注入，跨租户串数据
	 * （表现为不同货主看到同一份销售目标/销量进度）。此处显式捕获→在子线程重建→执行后清理，
	 * 保证每个并行统计任务都在正确租户下取数。
	 */
	private <T> CompletableFuture<T> supplyAsyncWithTenant(Long erpTenantId, Long wmsTenantId,
			Supplier<T> supplier) {
		return CompletableFuture.supplyAsync(() -> {
			TenantContext.setCurrentTenant(erpTenantId);
			WmsTenantContext.setCurrentWmsTenant(wmsTenantId);
			try {
				return supplier.get();
			}
			finally {
				TenantContext.clear();
				WmsTenantContext.clear();
			}
		});
	}

	/**
	 * 计算销售概览
	 *
	 * @param queryDTO 查询参数
	 * @return 销售概览数据
	 */
	private SalesOverviewVO calculateSalesOverview(DashboardQueryDTO queryDTO) {
		// 查询日期范围内的销售总额
		BigDecimal totalSalesRaw = dashboardMapper.sumTotalAmountRubByDateRange(queryDTO);
		BigDecimal totalSales = totalSalesRaw != null
				? totalSalesRaw.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		log.info("销售概览: 日期范围={} 到 {}, 总销售额={}", queryDTO.getStartDate(), queryDTO.getEndDate(), totalSales);

		return new SalesOverviewVO(totalSales);
	}

	/**
	 * 计算目标进度
	 *
	 * @param platform 平台筛选
	 * @return 目标进度数据
	 */
	private TargetProgressVO calculateTargetProgress(String platform) {
		LocalDate now = LocalDate.now();
		int currentYear = now.getYear();
		int currentMonth = now.getMonthValue();

		TargetProgressVO result = new TargetProgressVO();

		// 月度目标
		result.setMonthly(calculateMonthlyTarget(currentYear, currentMonth, platform));

		// 年度目标
		result.setYearly(calculateYearlyTarget(currentYear, platform));

		return result;
	}

	/**
	 * 计算月度目标进度
	 *
	 * @param year     年份
	 * @param month    月份
	 * @param platform 平台筛选
	 * @return 月度目标详情
	 */
	private TargetProgressVO.TargetDetailVO calculateMonthlyTarget(int year, int month, String platform) {
		TargetProgressVO.TargetDetailVO detail = new TargetProgressVO.TargetDetailVO();

		// 查询月度目标
		SalesTarget monthlyTarget = salesTargetMapper.selectByTargetYearAndTargetMonth(year, month);

		if (monthlyTarget == null || monthlyTarget.getTargetAmount() == null) {
			detail.setHasTarget(false);
			detail.setTarget(BigDecimal.ZERO);
			detail.setCurrent(BigDecimal.ZERO);
			detail.setProgress(BigDecimal.ZERO);
			log.info("月度目标: 未设置目标 {}-{}", year, month);
			return detail;
		}

		// 查询本月累计销售额
		BigDecimal monthlyCurrentRaw = dashboardMapper.sumTotalAmountRubByYearMonth(year, month, platform);
		BigDecimal monthlyCurrent = monthlyCurrentRaw != null
				? monthlyCurrentRaw.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// 计算完成百分比
		BigDecimal progress = BigDecimal.ZERO;
		if (monthlyTarget.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
			progress = monthlyCurrent.divide(monthlyTarget.getTargetAmount(), 4, RoundingMode.HALF_UP)
					.multiply(new BigDecimal("100"));
		}

		detail.setHasTarget(true);
		detail.setTarget(monthlyTarget.getTargetAmount());
		detail.setCurrent(monthlyCurrent);
		detail.setProgress(progress);

		log.info("月度目标: 目标={}, 当前={}, 完成度={}%", monthlyTarget.getTargetAmount(), monthlyCurrent, progress);

		return detail;
	}

	/**
	 * 计算年度目标进度
	 *
	 * @param year     年份
	 * @param platform 平台筛选
	 * @return 年度目标详情
	 */
	private TargetProgressVO.TargetDetailVO calculateYearlyTarget(int year, String platform) {
		TargetProgressVO.TargetDetailVO detail = new TargetProgressVO.TargetDetailVO();

		// 查询年度目标
		SalesTarget yearlyTarget = salesTargetMapper.selectYearlyTargetByYear(year);

		if (yearlyTarget == null || yearlyTarget.getTargetAmount() == null) {
			detail.setHasTarget(false);
			detail.setTarget(BigDecimal.ZERO);
			detail.setCurrent(BigDecimal.ZERO);
			detail.setProgress(BigDecimal.ZERO);
			log.info("年度目标: 未设置目标 {}", year);
			return detail;
		}

		// 查询本年累计销售额
		BigDecimal yearlyCurrentRaw = dashboardMapper.sumTotalAmountRubByYear(year, platform);
		BigDecimal yearlyCurrent = yearlyCurrentRaw != null
				? yearlyCurrentRaw.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// 计算完成百分比
		BigDecimal progress = BigDecimal.ZERO;
		if (yearlyTarget.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
			progress = yearlyCurrent.divide(yearlyTarget.getTargetAmount(), 4, RoundingMode.HALF_UP)
					.multiply(new BigDecimal("100"));
		}

		detail.setHasTarget(true);
		detail.setTarget(yearlyTarget.getTargetAmount());
		detail.setCurrent(yearlyCurrent);
		detail.setProgress(progress);

		log.info("年度目标: 目标={}, 当前={}, 完成度={}%", yearlyTarget.getTargetAmount(), yearlyCurrent, progress);

		return detail;
	}

	/**
	 * 获取汇率信息
	 *
	 * @return 汇率信息
	 */
	private ExchangeRatesVO getExchangeRates() {
		ExchangeRatesVO result = new ExchangeRatesVO();
		LocalDate today = LocalDate.now();

		// 主要货币列表（支持6种货币）
		List<String> currencies = Arrays.asList("EUR", "RUB", "BYN", "KZT", "KGS", "AMD", "USD");
		List<ExchangeRatesVO.RateItem> rateItems = new ArrayList<>();

		LocalDate rateDate = today;
		boolean foundAnyRate = false;

		// 尝试查询当日汇率，如果不存在则查询最近可用的汇率
		for (String currency : currencies) {
			ExchangeRate rate = exchangeRateMapper.getRateByDate(currency, "CNY", today);

			if (rate == null) {
				// 查询最近可用的汇率
				rate = exchangeRateMapper.getLatestRate(currency, "CNY");
			}

			if (rate != null) {
				ExchangeRatesVO.RateItem item = new ExchangeRatesVO.RateItem();
				item.setCurrency(currency);
				item.setRate(rate.getRate());
				rateItems.add(item);

				if (!foundAnyRate) {
					rateDate = rate.getRateDate();
					foundAnyRate = true;
				}
			}
		}

		result.setDate(rateDate);
		result.setRates(rateItems);

		log.info("汇率信息: 日期={}, 汇率数量={}", rateDate, rateItems.size());

		return result;
	}

	/**
	 * 计算平台履约类型销售分布
	 *
	 * @param queryDTO 查询参数
	 * @return 平台履约分布列表
	 */
	private List<PlatformFulfillmentVO> calculatePlatformFulfillment(DashboardQueryDTO queryDTO) {
		// 查询聚合数据
		List<PlatformFulfillmentStat> stats = dashboardMapper.aggregateByPlatformAndFulfillment(queryDTO);

		// 按平台分组
		Map<String, PlatformFulfillmentVO> platformMap = new HashMap<>();

		for (PlatformFulfillmentStat stat : stats) {
			String platformKey = stat.getPlatform();
			PlatformFulfillmentVO vo = platformMap.computeIfAbsent(platformKey, k -> {
				PlatformFulfillmentVO newVo = new PlatformFulfillmentVO();
				newVo.setPlatform(platformKey);
				newVo.setFbs(createEmptyDetail());
				newVo.setFbo(createEmptyDetail());
				newVo.setTotal(createEmptyDetail());
				return newVo;
			});

			// 金额转换（除以100）
			BigDecimal amount = stat.getTotalAmount() != null
					? stat.getTotalAmount().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO;

			// 根据履约类型填充数据
			String fulfillmentType = stat.getFulfillmentType();
			if ("FBS".equalsIgnoreCase(fulfillmentType)) {
				vo.getFbs().setCount(stat.getOrderCount());
				vo.getFbs().setAmount(amount);
			}
			else if ("FBO".equalsIgnoreCase(fulfillmentType)) {
				vo.getFbo().setCount(stat.getOrderCount());
				vo.getFbo().setAmount(amount);
			}

			// 累加到合计
			vo.getTotal().setCount(vo.getTotal().getCount() + stat.getOrderCount());
			vo.getTotal().setAmount(vo.getTotal().getAmount().add(amount));
		}

		List<PlatformFulfillmentVO> result = new ArrayList<>(platformMap.values());

		log.info("平台履约分布: 平台数量={}", result.size());

		return result;
	}

	/**
	 * 创建空的履约详情
	 *
	 * @return 履约详情
	 */
	private PlatformFulfillmentVO.FulfillmentDetail createEmptyDetail() {
		PlatformFulfillmentVO.FulfillmentDetail detail = new PlatformFulfillmentVO.FulfillmentDetail();
		detail.setCount(0L);
		detail.setAmount(BigDecimal.ZERO);
		return detail;
	}

	/**
	 * 计算销售趋势
	 *
	 * @param queryDTO 查询参数
	 * @return 销售趋势数据
	 */
	private SalesTrendVO calculateSalesTrend(DashboardQueryDTO queryDTO) {
		SalesTrendVO result = new SalesTrendVO();

		// 判断时间粒度
		long daysBetween = ChronoUnit.DAYS.between(queryDTO.getStartDate(), queryDTO.getEndDate());
		String granularity = daysBetween <= 1 ? "hour" : "day";
		result.setGranularity(granularity);

		// 查询趋势数据
		List<SalesTrendStat> stats;
		if ("hour".equals(granularity)) {
			stats = dashboardMapper.aggregateSalesByHour(queryDTO);
		}
		else {
			stats = dashboardMapper.aggregateSalesByDay(queryDTO);
		}

		// 转换数据（金额除以100）
		List<SalesTrendVO.TrendPoint> trendPoints = new ArrayList<>();
		for (SalesTrendStat stat : stats) {
			SalesTrendVO.TrendPoint point = new SalesTrendVO.TrendPoint();
			point.setTime(stat.getTimePoint());
			point.setTotalSales(stat.getTotalSales() != null
					? stat.getTotalSales().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO);
			point.setEffectiveSales(stat.getEffectiveSales() != null
					? stat.getEffectiveSales().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO);
			trendPoints.add(point);
		}

		result.setData(trendPoints);

		log.info("销售趋势: 粒度={}, 数据点数量={}", granularity, trendPoints.size());

		return result;
	}

	/**
	 * 计算订单状态分布
	 *
	 * @param queryDTO 查询参数
	 * @return 订单状态分布列表
	 */
	private List<OrderStatusVO> calculateOrderStatus(DashboardQueryDTO queryDTO) {
		// 查询聚合数据
		List<OrderStatusStat> stats = dashboardMapper.aggregateByOrderStatus(queryDTO);

		// 转换数据
		List<OrderStatusVO> result = new ArrayList<>();
		for (OrderStatusStat stat : stats) {
			OrderStatusVO vo = new OrderStatusVO();
			vo.setStatus(stat.getStatus());
			vo.setCount(stat.getOrderCount());
			vo.setAmount(stat.getTotalAmount() != null
					? stat.getTotalAmount().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO);
			result.add(vo);
		}

		log.info("订单状态分布: 状态数量={}", result.size());

		return result;
	}

	/**
	 * 计算SKU排名
	 *
	 * @param queryDTO 查询参数
	 * @return SKU排名数据
	 */
	private SkuRankingVO calculateSkuRanking(DashboardQueryDTO queryDTO) {
		List<ArticleSalesStat> articleStats = dashboardMapper.aggregateSalesByArticle(queryDTO);

		List<SkuRankingVO.SkuItem> sortedItems = articleStats.stream()
				.filter(stat -> stat.getQuantity() != null && stat.getQuantity() > 0)
				.map(stat -> {
					SkuRankingVO.SkuItem item = new SkuRankingVO.SkuItem();
					item.setSku(stat.getArticle());
					item.setIsMapped(Boolean.TRUE.equals(stat.getIsMapped()));
					item.setQuantity(stat.getQuantity());
					item.setAmount(stat.getTotalAmount() != null
							? stat.getTotalAmount().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
							: BigDecimal.ZERO);
					return item;
				})
				.collect(Collectors.toList());

		List<SkuRankingVO.SkuItem> top5 = sortedItems.stream().limit(5).collect(Collectors.toList());
		List<SkuRankingVO.SkuItem> bottom5 = new ArrayList<>();
		if (sortedItems.size() > 5) {
			bottom5 = sortedItems.subList(Math.max(0, sortedItems.size() - 5), sortedItems.size());
		}

		SkuRankingVO result = new SkuRankingVO();
		result.setTop5(top5);
		result.setBottom5(bottom5);

		log.info("SKU排名: TOP5数量={}, BOTTOM5数量={}", top5.size(), bottom5.size());

		return result;
	}

	/**
	 * 分页查询SKU销量排名
	 *
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	public PageResult<SkuRankingItemVO> getSkuRankingPage(PageParam pageParam, SkuRankingQO qo) {
		Page<SkuRankingItemVO> page = new Page<>(pageParam.getPage(), pageParam.getSize());

		// 从 PageParam 提取排序信息
		String sortField = "amount";
		String sortOrder = "desc";
		if (pageParam.getSorts() != null && !pageParam.getSorts().isEmpty()) {
			PageParam.Sort sort = pageParam.getSorts().get(0);
			String field = sort.getField();
			// 只允许 amount 和 quantity 两个字段排序，防止 SQL 注入
			if ("amount".equals(field) || "quantity".equals(field)) {
				sortField = field;
				sortOrder = sort.isAsc() ? "asc" : "desc";
			}
		}

		Page<SkuRankingItemVO> resultPage = dashboardMapper.selectSkuRankingPage(page, qo, sortField, sortOrder);

		// 设置排名序号
		List<SkuRankingItemVO> records = resultPage.getRecords();
		int startRank = (int) ((pageParam.getPage() - 1) * pageParam.getSize()) + 1;
		AtomicInteger rankCounter = new AtomicInteger(startRank);
		records.forEach(item -> item.setRank(rankCounter.getAndIncrement()));

		return new PageResult<>(records, resultPage.getTotal());
	}

	/**
	 * 查询SKU销量排名列表（用于导出）
	 *
	 * @param qo 查询条件
	 * @return SKU排名列表
	 */
	public List<SkuRankingItemVO> getSkuRankingList(SkuRankingQO qo) {
		// 导出默认按 amount 降序
		List<SkuRankingItemVO> list = dashboardMapper.selectSkuRankingList(qo, "amount", "desc");

		// 设置排名序号
		AtomicInteger rankCounter = new AtomicInteger(1);
		list.forEach(item -> item.setRank(rankCounter.getAndIncrement()));

		return list;
	}

	/**
	 * 获取SKU排名导出数据
	 *
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	public List<SkuRankingExportVO> getSkuRankingExportData(SkuRankingQO qo) {
		List<SkuRankingItemVO> list = getSkuRankingList(qo);
		return list.stream().map(this::convertToExportVO).collect(Collectors.toList());
	}

	/**
	 * 转换为导出VO
	 *
	 * @param item 排名项
	 * @return 导出VO
	 */
	private SkuRankingExportVO convertToExportVO(SkuRankingItemVO item) {
		SkuRankingExportVO exportVO = new SkuRankingExportVO();
		exportVO.setRank(item.getRank());
		exportVO.setSku(item.getSku());
		exportVO.setMappedStatus(Boolean.TRUE.equals(item.getIsMapped()) ? "已映射" : "未映射");
		exportVO.setSkuNameCn(item.getSkuNameCn());
		exportVO.setCategoryName(item.getCategoryName());
		exportVO.setQuantity(item.getQuantity());
		exportVO.setAmount(item.getAmount());
		return exportVO;
	}

}
