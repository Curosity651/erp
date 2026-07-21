package com.erp.admin.product.exception;

import com.erp.admin.product.enums.SkuResultCode;
import org.ballcat.common.core.exception.BusinessException;

/**
 * SKU通用业务异常
 *
 * @author ballcat
 */
public class SkuBusinessException extends BusinessException {

	private static final long serialVersionUID = 1L;

	public SkuBusinessException(SkuResultCode resultCode) {
		super(resultCode.getCode(), resultCode.getMessage());
	}

	public SkuBusinessException(SkuResultCode resultCode, String message) {
		super(resultCode.getCode(), message);
	}

	public SkuBusinessException(SkuResultCode resultCode, String message, Throwable cause) {
		super(resultCode.getCode(), message, cause);
	}

	public SkuBusinessException(int code, String message) {
		super(code, message);
	}

	public SkuBusinessException(int code, String message, Throwable cause) {
		super(code, message, cause);
	}

	/**
	 * 创建SKU编码重复异常
	 */
	public static SkuBusinessException skuCodeDuplicate(String skuCode) {
		return new SkuBusinessException(SkuResultCode.SKU_CODE_DUPLICATE, "SKU编码已存在: " + skuCode);
	}

	/**
	 * 创建SKU序号重复异常
	 */
	public static SkuBusinessException skuNoDuplicate(Integer skuNo) {
		return new SkuBusinessException(SkuResultCode.SKU_NO_DUPLICATE, "SKU序号已存在: " + skuNo);
	}

	/**
	 * 创建SKU不存在异常
	 */
	public static SkuBusinessException skuNotFound(Long id) {
		return new SkuBusinessException(SkuResultCode.SKU_NOT_FOUND, "SKU不存在，ID: " + id);
	}

	/**
	 * 创建SKU不存在异常
	 */
	public static SkuBusinessException skuNotFound(String skuCode) {
		return new SkuBusinessException(SkuResultCode.SKU_NOT_FOUND, "SKU不存在，编码: " + skuCode);
	}

}
