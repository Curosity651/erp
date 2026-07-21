package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 物流方式枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum ShippingMethod {

	/**
	 * 灰关
	 */
	GRAY("GRAY", "灰关"),

	/**
	 * 白关
	 */
	WHITE("WHITE", "白关");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static ShippingMethod fromCode(String code) {
		for (ShippingMethod method : values()) {
			if (method.getCode().equals(code)) {
				return method;
			}
		}
		throw new IllegalArgumentException("Invalid shipping method: " + code);
	}

}
