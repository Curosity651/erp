package com.erp.admin.platform.yandex.model.request.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Yandex 更新单个订单状态请求
 * <p>
 * PUT /v2/campaigns/{campaignId}/orders/{orderId}/status
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexUpdateOrderStatusRequest {

	/** 订单状态变更信息 */
	@JsonProperty("order")
	private OrderStatusChange order;

	@Data
	@Builder
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderStatusChange {

		/** 目标状态（PROCESSING / CANCELLED） */
		@JsonProperty("status")
		private String status;

		/** 目标子状态（READY_TO_SHIP / SHOP_FAILED 等） */
		@JsonProperty("substatus")
		private String substatus;
	}
}
