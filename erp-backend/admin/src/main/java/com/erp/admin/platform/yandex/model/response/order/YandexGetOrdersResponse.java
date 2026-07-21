package com.erp.admin.platform.yandex.model.response.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Yandex 订单列表响应
 * <p>
 * POST /v1/businesses/{businessId}/orders 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexGetOrdersResponse {

	/** 订单列表 */
	@JsonProperty("orders")
	private List<YandexOrder> orders;

	/** 分页信息 */
	@JsonProperty("paging")
	private Paging paging;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Paging {

		/** 下一页 token */
		@JsonProperty("nextPageToken")
		private String nextPageToken;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class YandexOrder {

		/** 订单 ID */
		@JsonProperty("orderId")
		private Long orderId;

		/** 店铺（campaign）ID */
		@JsonProperty("campaignId")
		private Long campaignId;

		/** 销售模式（FBS/FBY等） */
		@JsonProperty("programType")
		private String programType;

		/** 外部订单号 */
		@JsonProperty("externalOrderId")
		private String externalOrderId;

		/** 订单状态 */
		@JsonProperty("status")
		private String status;

		/** 订单子状态 */
		@JsonProperty("substatus")
		private String substatus;

		/** 创建时间（ISO 8601） */
		@JsonProperty("creationDate")
		private String creationDate;

		/** 更新时间（ISO 8601） */
		@JsonProperty("updateDate")
		private String updateDate;

		/** 支付类型（PREPAID/POSTPAID） */
		@JsonProperty("paymentType")
		private String paymentType;

		/** 支付方式 */
		@JsonProperty("paymentMethod")
		private String paymentMethod;

		/** 是否测试订单 */
		@JsonProperty("fake")
		private Boolean fake;

		/** 商品列表 */
		@JsonProperty("items")
		private List<OrderItem> items;

		/** 价格信息 */
		@JsonProperty("prices")
		private OrderPrice prices;

		/** 配送信息 */
		@JsonProperty("delivery")
		private OrderDelivery delivery;

		/** 买家类型 */
		@JsonProperty("buyerType")
		private String buyerType;

		/** 备注 */
		@JsonProperty("notes")
		private String notes;

		/** 是否有取消请求（DBS） */
		@JsonProperty("cancelRequested")
		private Boolean cancelRequested;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderItem {

		/** 商品在订单中的 ID */
		@JsonProperty("id")
		private Long id;

		/** 卖家 SKU（shopSku） */
		@JsonProperty("offerId")
		private String offerId;

		/** 商品名称 */
		@JsonProperty("offerName")
		private String offerName;

		/** 数量 */
		@JsonProperty("count")
		private Integer count;

		/** 价格信息 */
		@JsonProperty("prices")
		private ItemPrice prices;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ItemPrice {

		/** 商品总价（所有单位） */
		@JsonProperty("payment")
		private CurrencyValue payment;

		/** 平台补贴 */
		@JsonProperty("subsidy")
		private CurrencyValue subsidy;

		/** Plus 积分抵扣 */
		@JsonProperty("cashback")
		private CurrencyValue cashback;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CurrencyValue {

		/** 金额 */
		@JsonProperty("value")
		private BigDecimal value;

		/** 币种（RUR 等） */
		@JsonProperty("currencyId")
		private String currencyId;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderPrice {

		/** 买家支付金额 */
		@JsonProperty("payment")
		private CurrencyValue payment;

		/** 平台补贴 */
		@JsonProperty("subsidy")
		private CurrencyValue subsidy;

		/** Plus 积分抵扣 */
		@JsonProperty("cashback")
		private CurrencyValue cashback;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OrderDelivery {

		/** 配送方式（DELIVERY/PICKUP/POST） */
		@JsonProperty("type")
		private String type;

		/** 配送服务名称 */
		@JsonProperty("serviceName")
		private String serviceName;

		/** 配送服务 ID */
		@JsonProperty("deliveryServiceId")
		private Long deliveryServiceId;

		/** 仓库 ID */
		@JsonProperty("warehouseId")
		private String warehouseId;

		/** 配送合作类型 */
		@JsonProperty("deliveryPartnerType")
		private String deliveryPartnerType;

		/** 配送日期 */
		@JsonProperty("dates")
		private DeliveryDates dates;

		/** 发货信息 */
		@JsonProperty("shipment")
		private DeliveryShipment shipment;

		/** 是否估算日期 */
		@JsonProperty("estimated")
		private Boolean estimated;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DeliveryDates {

		/** 配送起始日期 */
		@JsonProperty("fromDate")
		private String fromDate;

		/** 配送截止日期 */
		@JsonProperty("toDate")
		private String toDate;

		/** 配送起始时间 */
		@JsonProperty("fromTime")
		private String fromTime;

		/** 配送截止时间 */
		@JsonProperty("toTime")
		private String toTime;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DeliveryShipment {

		/** 发货单 ID */
		@JsonProperty("id")
		private Long id;

		/** 发货日期 */
		@JsonProperty("shipmentDate")
		private String shipmentDate;
	}
}
