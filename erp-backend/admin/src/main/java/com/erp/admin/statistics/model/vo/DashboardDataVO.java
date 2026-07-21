package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Dashboard数据VO
 *
 * @author erp
 */
@Data
@Schema(title = "Dashboard数据")
public class DashboardDataVO {

	/**
	 * 销售概览
	 */
	@Schema(title = "销售概览")
	private SalesOverviewVO salesOverview;

	/**
	 * 目标进度
	 */
	@Schema(title = "目标进度")
	private TargetProgressVO targetProgress;

	/**
	 * 汇率信息
	 */
	@Schema(title = "汇率信息")
	private ExchangeRatesVO exchangeRates;

	/**
	 * 平台履约分布
	 */
	@Schema(title = "平台履约分布")
	private List<PlatformFulfillmentVO> platformFulfillment;

	/**
	 * 销售趋势
	 */
	@Schema(title = "销售趋势")
	private SalesTrendVO salesTrend;

	/**
	 * 订单状态分布
	 */
	@Schema(title = "订单状态分布")
	private List<OrderStatusVO> orderStatus;

	/**
	 * SKU排名
	 */
	@Schema(title = "SKU排名")
	private SkuRankingVO skuRanking;

}
