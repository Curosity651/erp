package com.erp.admin.wms.model.enums;

import java.util.Arrays;

/**
 * Reasons recorded for a warehouse location transfer.
 */
public enum LocationTransferReason {

	LOCATION_ORGANIZATION(true),
	BACKLOG_STAGING(true),
	OUTBOUND_PREPARATION(true),
	STOCKTAKE_CORRECTION(true),
	TEMPORARY_CLEARANCE(true),
	OTHER(true),
	OUTBOUND_PICKABLE_SHORTAGE(false);

	private final boolean manual;

	LocationTransferReason(boolean manual) {
		this.manual = manual;
	}

	public static boolean isManualReason(String code) {
		return Arrays.stream(values()).anyMatch(reason -> reason.manual && reason.name().equals(code));
	}

}
