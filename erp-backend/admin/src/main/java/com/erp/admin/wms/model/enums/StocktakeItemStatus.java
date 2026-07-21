package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点明细状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum StocktakeItemStatus {

	/**
	 * 未盘
	 */
	PENDING("PENDING", "未盘"),

	/**
	 * 已盘
	 */
	COUNTED("COUNTED", "已盘");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static StocktakeItemStatus fromCode(String code) {
		for (StocktakeItemStatus status : values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid stocktake item status: " + code);
	}

}
