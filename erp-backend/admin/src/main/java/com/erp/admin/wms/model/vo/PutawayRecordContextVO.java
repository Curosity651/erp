package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PutawayRecordContextVO {

	private Long inboundOrderId;

	private String inboundNo;

	private Long warehouseId;

	private Long erpTenantId;

	private String ownerName;

	private List<SkuSummaryVO> items = new ArrayList<>();

	private List<LocationSummaryVO> locations = new ArrayList<>();

	@Data
	public static class SkuSummaryVO {

		private String skuCode;

		private String warehouseSkuCode;

		private String skuName;

		private String imageUrl;

		private Integer receivedQuantity;

		private Integer outerLengthMm;

		private Integer outerWidthMm;

		private Integer outerHeightMm;

		private Integer outerGrossWeightG;

	}

	@Data
	public static class LocationSummaryVO {

		private Long locationId;

		private String locationCode;

		private String rackNo;

		private Long zoneId;

		private String zoneName;

		private String zoneType;

		private Integer publicShared;

		private boolean capacityCalculable;

		private Long capacityVolumeMm3;

		private Long occupiedVolumeMm3;

		private Long occupiedWeightGrams;

		private Long maxWeightGrams;

		private Integer skuKindCount;

		private Integer maxSkuKinds;

		private Boolean volumeAllowed;

		private Boolean weightAllowed;

		private Boolean skuKindsAllowed;

		private BigDecimal utilizationPercent;

	}

}
