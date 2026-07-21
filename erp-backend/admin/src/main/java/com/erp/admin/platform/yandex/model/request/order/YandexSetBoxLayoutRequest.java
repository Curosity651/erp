package com.erp.admin.platform.yandex.model.request.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Yandex 设置订单装箱布局请求
 * <p>
 * PUT /v2/campaigns/{campaignId}/orders/{orderId}/boxes
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexSetBoxLayoutRequest {

	/** 箱子列表 */
	@JsonProperty("boxes")
	private List<BoxLayout> boxes;

	/** 是否允许删除商品（从订单中移除缺货商品） */
	@JsonProperty("allowRemove")
	private Boolean allowRemove;

	@Data
	@Builder
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BoxLayout {

		/** 箱内商品列表 */
		@JsonProperty("items")
		private List<BoxLayoutItem> items;
	}

	@Data
	@Builder
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BoxLayoutItem {

		/** 商品在订单中的 ID */
		@JsonProperty("id")
		private Long id;

		/** 完整商品数量（与 partialCount 互斥） */
		@JsonProperty("fullCount")
		private Integer fullCount;

		/** 部分商品信息（与 fullCount 互斥） */
		@JsonProperty("partialCount")
		private PartialCount partialCount;
	}

	@Data
	@Builder
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PartialCount {

		/** 当前部分编号（从1开始） */
		@JsonProperty("current")
		private Integer current;

		/** 总共分成几个部分 */
		@JsonProperty("total")
		private Integer total;
	}
}
