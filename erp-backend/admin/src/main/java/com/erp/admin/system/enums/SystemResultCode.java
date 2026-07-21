package com.erp.admin.system.enums;

import org.ballcat.common.model.result.ResultCode;

/**
 * 系统相关错误码枚举
 *
 * @author ballcat
 */
public enum SystemResultCode implements ResultCode {

	/**
	 * OSS签名获取失败
	 */
	OSS_SIGNATURE_GENERATE_FAILED(50001, "OSS签名生成失败"),

	/**
	 * STS凭证获取失败
	 */
	STS_CREDENTIAL_FAILED(50002, "STS临时凭证获取失败"),

	/**
	 * OSS配置错误
	 */
	OSS_CONFIG_ERROR(50003, "OSS配置错误"),

	/**
	 * OSS服务不可用
	 */
	OSS_SERVICE_UNAVAILABLE(50004, "OSS服务不可用");

	private final int code;

	private final String message;

	SystemResultCode(int code, String message) {
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
