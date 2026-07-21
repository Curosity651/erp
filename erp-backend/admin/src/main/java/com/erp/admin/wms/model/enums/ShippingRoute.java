package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 物流线路枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum ShippingRoute {

	/**
	 * 东线
	 */
	EAST("EAST", "东线"),

	/**
	 * 西线
	 */
	WEST("WEST", "西线"),

	/**
	 * 铁路
	 */
	RAIL("RAIL", "铁路");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static ShippingRoute fromCode(String code) {
		for (ShippingRoute route : values()) {
			if (route.getCode().equals(code)) {
				return route;
			}
		}
		throw new IllegalArgumentException("Invalid shipping route: " + code);
	}

}
