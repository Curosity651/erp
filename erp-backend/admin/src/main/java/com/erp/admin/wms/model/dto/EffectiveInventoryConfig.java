package com.erp.admin.wms.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EffectiveInventoryConfig {
	private Integer safetyStock;
	private Boolean notifyEnabled;
	private Integer notifyThresholdDays;
	private String source;
}
