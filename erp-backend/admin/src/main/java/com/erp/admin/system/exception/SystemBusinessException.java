package com.erp.admin.system.exception;

import com.erp.admin.system.enums.SystemResultCode;
import org.ballcat.common.core.exception.BusinessException;

/**
 * 系统通用业务异常
 *
 * @author ballcat
 */
public class SystemBusinessException extends BusinessException {

	private static final long serialVersionUID = 1L;

	public SystemBusinessException(SystemResultCode resultCode) {
		super(resultCode.getCode(), resultCode.getMessage());
	}

	public SystemBusinessException(SystemResultCode resultCode, String message) {
		super(resultCode.getCode(), message);
	}

	public SystemBusinessException(SystemResultCode resultCode, String message, Throwable cause) {
		super(resultCode.getCode(), message, cause);
	}

	public SystemBusinessException(int code, String message) {
		super(code, message);
	}

	public SystemBusinessException(int code, String message, Throwable cause) {
		super(code, message, cause);
	}

	/**
	 * 创建OSS签名生成失败异常
	 */
	public static SystemBusinessException ossSignatureGenerateFailed(String message) {
		return new SystemBusinessException(SystemResultCode.OSS_SIGNATURE_GENERATE_FAILED, message);
	}

	/**
	 * 创建OSS签名生成失败异常
	 */
	public static SystemBusinessException ossSignatureGenerateFailed(String message, Throwable cause) {
		return new SystemBusinessException(SystemResultCode.OSS_SIGNATURE_GENERATE_FAILED, message, cause);
	}

	/**
	 * 创建STS凭证获取失败异常
	 */
	public static SystemBusinessException stsCredentialFailed(String message) {
		return new SystemBusinessException(SystemResultCode.STS_CREDENTIAL_FAILED, message);
	}

	/**
	 * 创建STS凭证获取失败异常
	 */
	public static SystemBusinessException stsCredentialFailed(String message, Throwable cause) {
		return new SystemBusinessException(SystemResultCode.STS_CREDENTIAL_FAILED, message, cause);
	}

	/**
	 * 创建OSS配置错误异常
	 */
	public static SystemBusinessException ossConfigError(String message) {
		return new SystemBusinessException(SystemResultCode.OSS_CONFIG_ERROR, message);
	}

	/**
	 * 创建OSS服务不可用异常
	 */
	public static SystemBusinessException ossServiceUnavailable(String message) {
		return new SystemBusinessException(SystemResultCode.OSS_SERVICE_UNAVAILABLE, message);
	}

}
