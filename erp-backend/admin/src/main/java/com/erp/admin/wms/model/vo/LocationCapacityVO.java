package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LocationCapacityVO {

	private Long locationId;

	private long capacityVolumeMm3;

	private long occupiedVolumeMm3;

	private long remainingVolumeMm3;

	private long occupiedWeightGrams;

	private long maxWeightGrams;

	private int skuKindCount;

	private int maxSkuKinds;

	private boolean volumeAllowed;

	private boolean weightAllowed;

	private boolean skuKindsAllowed;

	private BigDecimal utilizationPercent;

}
