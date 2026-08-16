package com.erp.admin.wms.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum FulfillmentStatus {
	DRAFT,
	WAITING_SHELF,
	PLATFORM_PROCESSING,
	WAITING_PICK,
	PICKING,
	WAITING_PACK,
	PACKED,
	SHIPPED,
	CANCEL_RETURNING,
	CANCELLED,
	EXCEPTION;

	public boolean canTransitTo(FulfillmentStatus target) {
		return allowedTargets().contains(target);
	}

	private Set<FulfillmentStatus> allowedTargets() {
		switch (this) {
			case DRAFT:
				return EnumSet.of(WAITING_SHELF, CANCELLED);
			case WAITING_SHELF:
				return EnumSet.of(PLATFORM_PROCESSING, CANCELLED, EXCEPTION);
			case PLATFORM_PROCESSING:
				return EnumSet.of(WAITING_PICK, CANCELLED, EXCEPTION);
			case WAITING_PICK:
				return EnumSet.of(PICKING, CANCELLED, EXCEPTION);
			case PICKING:
				return EnumSet.of(WAITING_PACK, CANCEL_RETURNING, EXCEPTION);
			case WAITING_PACK:
				return EnumSet.of(PACKED, CANCEL_RETURNING, EXCEPTION);
			case PACKED:
				return EnumSet.of(SHIPPED, CANCEL_RETURNING, EXCEPTION);
			case CANCEL_RETURNING:
				return EnumSet.of(CANCELLED, EXCEPTION);
			case EXCEPTION:
				return EnumSet.of(WAITING_SHELF, PLATFORM_PROCESSING, WAITING_PICK, PICKING,
					WAITING_PACK, PACKED, CANCEL_RETURNING, CANCELLED);
			default:
				return EnumSet.noneOf(FulfillmentStatus.class);
		}
	}
}
