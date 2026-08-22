package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class LocationInventoryGridVO {

	private Long locationId;

	private Long warehouseId;

	private String rackNo;

	private Integer sequenceNo;

	private String locationCode;

	private Long zoneId;

	private String zoneName;

	private String zoneType;

	private String locationType;

	private Integer publicShared;

	private long capacityVolumeMm3;

	private long usedVolumeMm3;

	private long maxWeightGrams;

	private long usedWeightGrams;

	private BigDecimal utilizationPercent;

	private int skuKindCount;

	private List<String> skuCodes;

	private int totalQuantity;

	private int reservedQuantity;

	private int availableQuantity;

	private boolean volumeExceeded;

	private boolean weightExceeded;

	private boolean capacityDataComplete;

}
