package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LocationInventoryGridVO {

	private Long locationId;

	private Long warehouseId;

	private String rackNo;

	private Integer sequenceNo;

	private String locationCode;

	private String locationType;

	private Integer publicShared;

	private long capacityVolumeMm3;

	private long usedVolumeMm3;

	private long maxWeightGrams;

	private long usedWeightGrams;

	private BigDecimal utilizationPercent;

	private int skuKindCount;

	private int totalQuantity;

	private int reservedQuantity;

	private int availableQuantity;

	private boolean volumeExceeded;

	private boolean weightExceeded;

	private boolean capacityDataComplete;

}
