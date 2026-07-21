package com.erp.admin.tenant.exception;

import com.erp.admin.tenant.enums.TenantResultCode;
import org.ballcat.common.core.exception.BusinessException;

/**
 * 租户通用业务异常
 *
 * @author erp
 */
public class TenantBusinessException extends BusinessException {

	private static final long serialVersionUID = 1L;

	public TenantBusinessException(TenantResultCode resultCode) {
		super(resultCode.getCode(), resultCode.getMessage());
	}

	public TenantBusinessException(TenantResultCode resultCode, String message) {
		super(resultCode.getCode(), message);
	}

	public TenantBusinessException(int code, String message) {
		super(code, message);
	}

}
