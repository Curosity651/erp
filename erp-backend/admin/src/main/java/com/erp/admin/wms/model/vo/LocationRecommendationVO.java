package com.erp.admin.wms.model.vo;

import lombok.Data;

@Data
public class LocationRecommendationVO {

	private Long locationId;

	private String locationCode;

	private String rackNo;

	private Integer sequenceNo;

	private String locationType;

	private Integer publicShared;

	private int recommendedQuantity;

	private int maxByGeometry;

	private int maxByVolume;

	private int maxByWeight;

	private long remainingVolumeMm3;

	private boolean weightAllowed;

	private boolean skuKindsAllowed;

}
