package com.erp.admin.platform.yandex.model.request.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Yandex 订单列表请求
 * <p>
 * POST /v1/businesses/{businessId}/orders
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexGetOrdersRequest {

	/** 订单 ID 列表（最多50个） */
	@JsonProperty("orderIds")
	private Set<Long> orderIds;

	/** 店铺（campaign）ID 列表 */
	@JsonProperty("campaignIds")
	private Set<Long> campaignIds;

	/** 订单状态过滤 */
	@JsonProperty("statuses")
	private Set<String> statuses;

	/** 订单子状态过滤 */
	@JsonProperty("substatuses")
	private Set<String> substatuses;

	/** 日期过滤 */
	@JsonProperty("dates")
	private DatesFilter dates;

	/** 销售模式过滤（FBS/FBY等） */
	@JsonProperty("programTypes")
	private Set<String> programTypes;

	/** 是否测试订单 */
	@JsonProperty("fake")
	private Boolean fake;

	@Data
	@Builder
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DatesFilter {

		/** 创建日期起始（yyyy-MM-dd） */
		@JsonProperty("creationDateFrom")
		private String creationDateFrom;

		/** 创建日期截止（yyyy-MM-dd） */
		@JsonProperty("creationDateTo")
		private String creationDateTo;

		/** 更新时间起始（ISO 8601） */
		@JsonProperty("updateDateFrom")
		private String updateDateFrom;

		/** 更新时间截止（ISO 8601） */
		@JsonProperty("updateDateTo")
		private String updateDateTo;

		/** 发货日期起始（yyyy-MM-dd） */
		@JsonProperty("shipmentDateFrom")
		private String shipmentDateFrom;

		/** 发货日期截止（yyyy-MM-dd） */
		@JsonProperty("shipmentDateTo")
		private String shipmentDateTo;
	}
}
