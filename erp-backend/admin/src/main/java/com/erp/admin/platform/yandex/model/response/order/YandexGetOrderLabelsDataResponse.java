package com.erp.admin.platform.yandex.model.response.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Yandex 获取面单数据响应
 * <p>
 * GET /v2/campaigns/{campaignId}/orders/{orderId}/delivery/labels/data 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexGetOrderLabelsDataResponse {

	/** API 状态 */
	@JsonProperty("status")
	private String status;

	/** 面单数据 */
	@JsonProperty("result")
	private OrderLabel result;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderLabel {

		/** 订单 ID */
		@JsonProperty("orderId")
		private Long orderId;

		/** 箱子数量 */
		@JsonProperty("placesNumber")
		private Integer placesNumber;

		/** 各箱子面单信息 */
		@JsonProperty("parcelBoxLabels")
		private List<BoxLabel> parcelBoxLabels;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BoxLabel {

		/** 面单 URL */
		@JsonProperty("url")
		private String url;

		/** 卖家名称 */
		@JsonProperty("supplierName")
		private String supplierName;

		/** 配送服务名称 */
		@JsonProperty("deliveryServiceName")
		private String deliveryServiceName;

		/** 订单 ID */
		@JsonProperty("orderId")
		private Long orderId;

		/** 订单号（卖家系统中的编号） */
		@JsonProperty("orderNum")
		private String orderNum;

		/** 收件人姓名 */
		@JsonProperty("recipientName")
		private String recipientName;

		/** 箱子 ID */
		@JsonProperty("boxId")
		private Long boxId;

		/** 履约 ID（格式: 订单号-箱号） */
		@JsonProperty("fulfilmentId")
		private String fulfilmentId;

		/** 位置编号（如 1/3） */
		@JsonProperty("place")
		private String place;

		/** 配送服务 ID */
		@JsonProperty("deliveryServiceId")
		private String deliveryServiceId;

		/** 配送地址 */
		@JsonProperty("deliveryAddress")
		private String deliveryAddress;

		/** 发货日期（dd.MM.yyyy） */
		@JsonProperty("shipmentDate")
		private String shipmentDate;
	}
}
