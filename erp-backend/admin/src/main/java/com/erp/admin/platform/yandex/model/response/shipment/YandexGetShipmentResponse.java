package com.erp.admin.platform.yandex.model.response.shipment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

/**
 * Yandex 获取单个发货单详情响应
 * <p>
 * GET /v2/campaigns/{campaignId}/first-mile/shipments/{shipmentId} 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexGetShipmentResponse {

	/** API 状态 */
	@JsonProperty("status")
	private String status;

	/** 发货单详情 */
	@JsonProperty("result")
	private ShipmentDetail result;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ShipmentDetail {

		/** 发货单 ID */
		@JsonProperty("id")
		private Long id;

		/** 计划发货开始时间（ISO 8601） */
		@JsonProperty("planIntervalFrom")
		private String planIntervalFrom;

		/** 计划发货结束时间（ISO 8601） */
		@JsonProperty("planIntervalTo")
		private String planIntervalTo;

		/** 发货方式 */
		@JsonProperty("shipmentType")
		private String shipmentType;

		/** 发货仓库 */
		@JsonProperty("warehouse")
		private ShipmentWarehouse warehouse;

		/** 目标仓库 */
		@JsonProperty("warehouseTo")
		private ShipmentWarehouse warehouseTo;

		/** 外部 ID */
		@JsonProperty("externalId")
		private String externalId;

		/** 订单 ID 列表 */
		@JsonProperty("orderIds")
		private Set<Long> orderIds;

		/** 草稿数量 */
		@JsonProperty("draftCount")
		private Integer draftCount;

		/** 已确认数量 */
		@JsonProperty("plannedCount")
		private Integer plannedCount;

		/** 实际接收数量 */
		@JsonProperty("factCount")
		private Integer factCount;

		/** 当前状态 */
		@JsonProperty("currentStatus")
		private StatusChange currentStatus;

		/** 可用操作列表 */
		@JsonProperty("availableActions")
		private Set<String> availableActions;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StatusChange {

		/** 状态 */
		@JsonProperty("status")
		private String status;

		/** 状态描述 */
		@JsonProperty("description")
		private String description;

		/** 更新时间（ISO 8601） */
		@JsonProperty("updateTime")
		private String updateTime;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ShipmentWarehouse {

		/** 仓库 ID */
		@JsonProperty("id")
		private Long id;

		/** 仓库名称 */
		@JsonProperty("name")
		private String name;
	}
}
