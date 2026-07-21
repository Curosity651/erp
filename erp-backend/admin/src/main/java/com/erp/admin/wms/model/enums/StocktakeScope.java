package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点范围枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum StocktakeScope {

	/**
	 * 全部SKU
	 */
	ALL("ALL", "全部SKU"),

	/**
	 * 指定SKU
	 */
	PARTIAL("PARTIAL", "指定SKU");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static StocktakeScope fromCode(String code) {
		for (StocktakeScope scope : values()) {
			if (scope.getCode().equals(code)) {
				return scope;
			}
		}
		throw new IllegalArgumentException("Invalid stocktake scope: " + code);
	}

}
