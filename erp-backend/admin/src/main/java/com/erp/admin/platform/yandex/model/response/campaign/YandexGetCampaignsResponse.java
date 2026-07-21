package com.erp.admin.platform.yandex.model.response.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Yandex 获取店铺列表响应
 * <p>
 * GET /v2/campaigns 的响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexGetCampaignsResponse {

	/** 店铺列表 */
	@JsonProperty("campaigns")
	private List<Campaign> campaigns;

	/** 分页信息 */
	@JsonProperty("pager")
	private Pager pager;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Campaign {

		/** 店铺（campaign）ID */
		@JsonProperty("id")
		private Long id;

		/** 店铺域名/名称 */
		@JsonProperty("domain")
		private String domain;

		/** 所属 business 信息 */
		@JsonProperty("business")
		private Business business;

		/** 放置类型（FBS/FBY/DBS等） */
		@JsonProperty("placementType")
		private String placementType;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Business {

		/** Business ID */
		@JsonProperty("id")
		private Long id;

		/** Business 名称 */
		@JsonProperty("name")
		private String name;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Pager {

		/** 当前页码 */
		@JsonProperty("currentPage")
		private Integer currentPage;

		/** 总页数 */
		@JsonProperty("pagesCount")
		private Integer pagesCount;

		/** 每页数量 */
		@JsonProperty("pageSize")
		private Integer pageSize;

		/** 总数量 */
		@JsonProperty("total")
		private Integer total;
	}
}
