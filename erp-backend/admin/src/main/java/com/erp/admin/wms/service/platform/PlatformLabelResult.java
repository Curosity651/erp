package com.erp.admin.wms.service.platform;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlatformLabelResult extends PlatformActionResult {
	private String labelUrl;
	private String labelBarcode;

	public PlatformLabelResult(boolean success, String message, String externalReference,
			String labelUrl, String labelBarcode) {
		super(success, message, externalReference);
		this.labelUrl = labelUrl;
		this.labelBarcode = labelBarcode;
	}

	public static PlatformLabelResult success(String externalReference, String labelUrl, String labelBarcode) {
		return new PlatformLabelResult(true, "OK", externalReference, labelUrl, labelBarcode);
	}
}

