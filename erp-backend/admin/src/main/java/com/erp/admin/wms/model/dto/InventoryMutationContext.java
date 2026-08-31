package com.erp.admin.wms.model.dto;

import com.erp.admin.wms.model.enums.InventoryEventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryMutationContext {
	private InventoryEventType eventType;
	private String sourceType;
	private Long sourceId;
	private String sourceNo;
	private String reason;
	private Long operatorId;
	private String operatorName;
	private String idempotencyKey;
}
