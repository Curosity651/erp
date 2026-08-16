package com.erp.admin.wms.service.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformActionResult {
	private boolean success;
	private String message;
	private String externalReference;

	public static PlatformActionResult success(String message, String externalReference) {
		return new PlatformActionResult(true, message, externalReference);
	}

	public static PlatformActionResult failure(String message) {
		return new PlatformActionResult(false, message, null);
	}
}

