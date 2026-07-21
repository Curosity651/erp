package com.erp.admin.platform.yandex.model.response.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Yandex 设置装箱布局响应
 * <p>
 * PUT /v2/campaigns/{campaignId}/orders/{orderId}/boxes 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexSetBoxLayoutResponse {

	/** API 状态 */
	@JsonProperty("status")
	private String status;

	/** 结果 */
	@JsonProperty("result")
	private Result result;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		/** 箱子列表（带平台分配的箱子 ID） */
		@JsonProperty("boxes")
		private List<EnrichedBoxLayout> boxes;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EnrichedBoxLayout {

		/** 平台分配的箱子 ID */
		@JsonProperty("boxId")
		private Long boxId;

		/** 箱内商品列表 */
		@JsonProperty("items")
		private List<BoxLayoutItem> items;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BoxLayoutItem {

		/** 商品在订单中的 ID */
		@JsonProperty("id")
		private Long id;

		/** 完整商品数量 */
		@JsonProperty("fullCount")
		private Integer fullCount;
	}
}
