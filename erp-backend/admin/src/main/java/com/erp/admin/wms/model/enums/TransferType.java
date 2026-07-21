package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调拨类型枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum TransferType {

	/**
	 * 普通调拨（自有仓之间调拨，系统内唯一调拨类型）
	 */
	NORMAL("NORMAL", "普通调拨");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static TransferType fromCode(String code) {
		for (TransferType type : values()) {
			if (type.getCode().equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid transfer type: " + code);
	}

}
