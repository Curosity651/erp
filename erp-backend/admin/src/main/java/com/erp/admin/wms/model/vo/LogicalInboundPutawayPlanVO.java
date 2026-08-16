package com.erp.admin.wms.model.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class LogicalInboundPutawayPlanVO {

	private Long inboundOrderId;

	private Long warehouseId;

	private List<SkuPlanVO> items = new ArrayList<>();

	@Data
	public static class SkuPlanVO {

		private String skuCode;

		private String skuName;

		private Integer receivedQuantity;

		private Integer outerLengthMm;

		private Integer outerWidthMm;

		private Integer outerHeightMm;

		private Integer outerGrossWeightG;

		private List<LocationRecommendationVO> recommendations = new ArrayList<>();

	}

}
