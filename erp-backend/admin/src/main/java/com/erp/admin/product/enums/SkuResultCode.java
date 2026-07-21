package com.erp.admin.product.enums;

import org.ballcat.common.model.result.ResultCode;

/**
 * SKU相关错误码枚举
 *
 * @author ballcat
 */
public enum SkuResultCode implements ResultCode {

	/**
	 * SKU编码重复
	 */
	SKU_CODE_DUPLICATE(40001, "SKU编码已存在"),

	/**
	 * SKU序号重复
	 */
	SKU_NO_DUPLICATE(40002, "SKU序号已存在"),

	/**
	 * SKU不存在
	 */
	SKU_NOT_FOUND(40003, "SKU不存在"),

	/**
	 * 无效文件类型
	 */
	INVALID_FILE_TYPE(40004, "不支持的文件类型"),

	/**
	 * 文件大小超出限制
	 */
	FILE_SIZE_EXCEEDED(40005, "文件大小超出限制"),

	/**
	 * SKU编码格式不正确
	 */
	INVALID_SKU_CODE_FORMAT(40006, "SKU编码只能包含大写字母、数字、横线和下划线，长度3-100"),

	/**
	 * 税率验证失败
	 */
	INVALID_TAX_RATE(40007, "含税时税率必须填写"),

	/**
	 * SKU状态不允许操作
	 */
	SKU_STATUS_NOT_ALLOWED(40008, "当前SKU状态不允许此操作"),

	/**
	 * SKU关联文件不存在
	 */
	SKU_FILE_NOT_FOUND(40009, "SKU关联文件不存在");

	private final int code;

	private final String message;

	SkuResultCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public Integer getCode() {
		return this.code;
	}

	@Override
	public String getMessage() {
		return this.message;
	}

}
