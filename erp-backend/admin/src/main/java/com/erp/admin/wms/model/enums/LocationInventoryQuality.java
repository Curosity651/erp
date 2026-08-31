package com.erp.admin.wms.model.enums;

import org.springframework.util.Assert;

import java.util.Locale;

public enum LocationInventoryQuality {
	GOOD,
	DEFECTIVE;

	public static String normalize(String value) {
		Assert.hasText(value, "库存品质不能为空");
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if ("DAMAGED".equals(normalized)) {
			return DEFECTIVE.name();
		}
		Assert.isTrue(GOOD.name().equals(normalized) || DEFECTIVE.name().equals(normalized),
				"库存品质仅支持 GOOD 或 DEFECTIVE");
		return normalized;
	}
}
