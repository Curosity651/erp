package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 物流单状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum ShippingStatus {

	/**
	 * 待发货
	 */
	PENDING("PENDING", "待发货"),

	/**
	 * 已发货
	 */
	SHIPPED("SHIPPED", "已发货"),

	/**
	 * 部分到货
	 */
	PARTIAL_ARRIVED("PARTIAL_ARRIVED", "部分到货"),

	/**
	 * 全部到货
	 */
	ALL_ARRIVED("ALL_ARRIVED", "全部到货"),

	/**
	 * 已完成
	 */
	COMPLETED("COMPLETED", "已完成");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static ShippingStatus fromCode(String code) {
		for (ShippingStatus status : values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid shipping status: " + code);
	}

}
