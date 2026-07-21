package com.erp.admin.platform.yandex.model.response.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Yandex 获取仓库列表响应
 * <p>
 * GET /v2/businesses/{businessId}/warehouses 的响应体（旧版）
 * POST /v2/businesses/{businessId}/warehouses 的响应体（新版分页）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexGetWarehousesResponse {

	/** API 状态 */
	@JsonProperty("status")
	private String status;

	/** 结果 */
	@JsonProperty("result")
	private Result result;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		/** 仓库列表 */
		@JsonProperty("warehouses")
		private List<Warehouse> warehouses;

		/** 仓库组列表（旧版 GET 接口返回） */
		@JsonProperty("warehouseGroups")
		private List<WarehouseGroup> warehouseGroups;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Warehouse {

		/** 仓库 ID */
		@JsonProperty("id")
		private Long id;

		/** 仓库名称 */
		@JsonProperty("name")
		private String name;

		/** 所属店铺（campaign）ID */
		@JsonProperty("campaignId")
		private Long campaignId;

		/** 是否支持快速配送 */
		@JsonProperty("express")
		private Boolean express;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class WarehouseGroup {

		/** 组名称 */
		@JsonProperty("name")
		private String name;

		/** 主仓库（用于传递库存） */
		@JsonProperty("mainWarehouse")
		private Warehouse mainWarehouse;

		/** 组内仓库列表 */
		@JsonProperty("warehouses")
		private List<Warehouse> warehouses;
	}
}
