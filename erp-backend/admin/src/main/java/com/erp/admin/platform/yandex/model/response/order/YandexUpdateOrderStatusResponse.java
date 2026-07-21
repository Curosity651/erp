package com.erp.admin.platform.yandex.model.response.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Yandex 更新单个订单状态响应
 * <p>
 * PUT /v2/campaigns/{campaignId}/orders/{orderId}/status 的响应体
 * <p>
 * 返回更新后的订单信息（使用 v2 OrderDTO 格式，字段较多，这里只取核心字段）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexUpdateOrderStatusResponse {

	/** 更新后的订单信息 */
	@JsonProperty("order")
	private UpdatedOrder order;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UpdatedOrder {

		/** 订单 ID */
		@JsonProperty("id")
		private Long id;

		/** 更新后的状态 */
		@JsonProperty("status")
		private String status;

		/** 更新后的子状态 */
		@JsonProperty("substatus")
		private String substatus;
	}
}
