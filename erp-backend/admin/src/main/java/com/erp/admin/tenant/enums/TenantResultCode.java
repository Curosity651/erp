package com.erp.admin.tenant.enums;

import org.ballcat.common.model.result.ResultCode;

/**
 * 租户相关错误码枚举
 *
 * @author erp
 */
public enum TenantResultCode implements ResultCode {

	/**
	 * 无租户上下文
	 */
	NO_TENANT_CONTEXT(50003, "无租户上下文，拒绝访问"),

	/**
	 * 登录入口与账号身份不匹配
	 */
	ENTRY_TYPE_MISMATCH(50004, "请使用正确入口登录"),

	/**
	 * 当前用户未绑定租户
	 */
	USER_TENANT_NOT_BOUND(50005, "当前用户未绑定租户"),

	/**
	 * 租户编码已存在
	 */
	TENANT_CODE_EXISTS(50006, "租户编码已存在"),

	/**
	 * 用户名已存在
	 */
	USERNAME_EXISTS(50007, "用户名已存在"),

	/**
	 * 无权开通该类型租户
	 */
	OPEN_TENANT_FORBIDDEN(50008, "无权开通该类型租户"),

	/**
	 * 租户已停用（账号被停用，禁止登录/访问）
	 */
	TENANT_DISABLED(50009, "账号已停用，请联系管理员");

	private final int code;

	private final String message;

	TenantResultCode(int code, String message) {
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
