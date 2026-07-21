package com.erp.admin.system.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum StorageType {

	/**
	 * 公有桶 - 可直接访问
	 */
	PUBLIC("公有桶"),

	/**
	 * 私有桶 - 需签名访问
	 */
	PRIVATE("私有桶");

	private final String description;

}
