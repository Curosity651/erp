package com.erp.admin.platform.yandex.model.response.shipment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Yandex 搜索发货单响应
 * <p>
 * PUT /v2/campaigns/{campaignId}/first-mile/shipments 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexSearchShipmentsResponse {

	/** API 状态 */
	@JsonProperty("status")
	private String status;

	/** 结果 */
	@JsonProperty("result")
	private Result result;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		/** 发货单列表 */
		@JsonProperty("shipments")
		private List<ShipmentInfo> shipments;

		/** 分页信息 */
		@JsonProperty("paging")
		private Paging paging;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Paging {

		/** 下一页 token */
		@JsonProperty("nextPageToken")
		private String nextPageToken;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ShipmentInfo {

		/** 发货单 ID */
		@JsonProperty("id")
		private Long id;

		/** 计划发货开始时间（ISO 8601） */
		@JsonProperty("planIntervalFrom")
		private String planIntervalFrom;

		/** 计划发货结束时间（ISO 8601） */
		@JsonProperty("planIntervalTo")
		private String planIntervalTo;

		/** 发货方式（IMPORT/WITHDRAW） */
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

		/** 发货单中的订单 ID 列表 */
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

		/** 发货单状态 */
		@JsonProperty("status")
		private String status;

		/** 状态描述 */
		@JsonProperty("statusDescription")
		private String statusDescription;

		/** 状态更新时间（ISO 8601） */
		@JsonProperty("statusUpdateTime")
		private String statusUpdateTime;
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
