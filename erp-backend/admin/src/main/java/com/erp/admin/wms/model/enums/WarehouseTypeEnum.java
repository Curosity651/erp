package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 仓库类型枚举
 *
 * @author erp
 * @since 2024-12-22
 */
@Getter
@AllArgsConstructor
public enum WarehouseTypeEnum {

	/**
	 * 自有仓
	 */
	OWN("OWN", "自有仓"),

	/**
	 * FBO仓
	 */
	FBO("FBO", "FBO仓");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static WarehouseTypeEnum fromCode(String code) {
		for (WarehouseTypeEnum type : values()) {
			if (type.getCode().equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid warehouse type: " + code);
	}

}