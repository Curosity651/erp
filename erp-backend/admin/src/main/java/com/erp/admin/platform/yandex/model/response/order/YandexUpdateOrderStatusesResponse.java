package com.erp.admin.platform.yandex.model.response.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Yandex 批量更新订单状态响应
 * <p>
 * POST /v2/campaigns/{campaignId}/orders/status-update 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexUpdateOrderStatusesResponse {

	/** API 状态 */
	@JsonProperty("status")
	private String status;

	/** 结果 */
	@JsonProperty("result")
	private Result result;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		/** 更新结果列表 */
		@JsonProperty("orders")
		private List<OrderUpdateResult> orders;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderUpdateResult {

		/** 订单 ID */
		@JsonProperty("id")
		private Long id;

		/** 更新后的状态 */
		@JsonProperty("status")
		private String status;

		/** 更新后的子状态 */
		@JsonProperty("substatus")
		private String substatus;

		/** 更新状态：OK 或 ERROR */
		@JsonProperty("updateStatus")
		private String updateStatus;

		/** 错误详情（当 updateStatus=ERROR 时） */
		@JsonProperty("errorDetails")
		private String errorDetails;
	}
}
