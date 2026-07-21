package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调拨单状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum TransferOrderStatus {

	/**
	 * 草稿
	 */
	DRAFT("DRAFT", "草稿"),

	/**
	 * 在途
	 */
	IN_TRANSIT("IN_TRANSIT", "在途"),

	/**
	 * 已入库
	 */
	COMPLETED("COMPLETED", "已入库"),

	/**
	 * 已取消
	 */
	CANCELLED("CANCELLED", "已取消"),

	/**
	 * 已撤回（在途状态撤回）
	 */
	REVOKED("REVOKED", "已撤回");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static TransferOrderStatus fromCode(String code) {
		for (TransferOrderStatus status : values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid transfer order status: " + code);
	}

	/**
	 * 是否可删除（仅草稿和已取消状态）
	 */
	public boolean isDeletable() {
		return this == DRAFT || this == CANCELLED;
	}

}
