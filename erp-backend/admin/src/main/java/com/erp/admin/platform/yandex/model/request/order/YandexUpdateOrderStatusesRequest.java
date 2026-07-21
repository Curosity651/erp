package com.erp.admin.platform.yandex.model.request.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Yandex 批量更新订单状态请求
 * <p>
 * POST /v2/campaigns/{campaignId}/orders/status-update
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexUpdateOrderStatusesRequest {

	/** 订单状态变更列表（最多30个） */
	@JsonProperty("orders")
	private List<OrderState> orders;

	@Data
	@Builder
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderState {

		/** 订单 ID */
		@JsonProperty("id")
		private Long id;

		/** 目标状态 */
		@JsonProperty("status")
		private String status;

		/** 目标子状态 */
		@JsonProperty("substatus")
		private String substatus;
	}
}
