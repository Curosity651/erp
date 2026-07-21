package com.erp.admin.product.exception.handler;

import java.sql.SQLIntegrityConstraintViolationException;

import com.erp.admin.product.enums.SkuResultCode;
import com.erp.admin.product.exception.SkuBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * SKU相关异常处理器
 *
 * @author ballcat
 */
@Slf4j
@RestControllerAdvice
public class SkuExceptionHandler {

	/**
	 * 处理SKU业务异常
	 */
	@ExceptionHandler(SkuBusinessException.class)
	public ApiResult<Void> handleSkuBusinessException(SkuBusinessException e) {
		log.warn("SKU业务异常: code={}, message={}", e.getCode(), e.getMessage());
		return ApiResult.failed(e.getCode(), e.getMessage());
	}

	/**
	 * 处理数据库唯一索引约束异常
	 */
	@ExceptionHandler(DuplicateKeyException.class)
	public ApiResult<Void> handleDuplicateKeyException(DuplicateKeyException e) {
		log.error("数据库唯一索引约束异常", e);

		String message = e.getMessage();
		if (message != null) {
			// 检查是否是SKU编码重复
			if (message.contains("sku_code") || message.contains("UK_SKU_CODE")) {
				return ApiResult.failed(SkuResultCode.SKU_CODE_DUPLICATE.getCode(), "SKU编码已存在，请检查数据唯一性");
			}
			// 检查是否是SKU序号重复
			if (message.contains("sku_no") || message.contains("UK_SKU_NO")) {
				return ApiResult.failed(SkuResultCode.SKU_NO_DUPLICATE.getCode(), "SKU序号已存在，请检查数据唯一性");
			}
		}

		return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR.getCode(), "数据保存失败，存在重复数据，请检查SKU编码和序号的唯一性");
	}

	/**
	 * 处理SQL完整性约束异常
	 */
	@ExceptionHandler(SQLIntegrityConstraintViolationException.class)
	public ApiResult<Void> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
		log.error("SQL完整性约束异常", e);

		String message = e.getMessage();
		if (message != null) {
			// 处理 update_time 为空的友好提示
			if (message.contains("update_time") && message.contains("cannot be null")) {
				return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR.getCode(), "系统缺少更新时间字段，请稍后再试或联系管理员");
			}
			// 检查是否是SKU编码重复
			if (message.contains("sku_code") || message.contains("UK_SKU_CODE")) {
				return ApiResult.failed(SkuResultCode.SKU_CODE_DUPLICATE.getCode(), "SKU编码已存在，请使用不同的编码");
			}
			// 检查是否是SKU序号重复
			if (message.contains("sku_no") || message.contains("UK_SKU_NO")) {
				return ApiResult.failed(SkuResultCode.SKU_NO_DUPLICATE.getCode(), "SKU序号已存在，请使用不同的序号");
			}
			// 检查是否是重复主键
			if (message.contains("Duplicate entry") && message.contains("PRIMARY")) {
				return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR.getCode(), "数据已存在，无法重复添加");
			}
		}

		return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR.getCode(), "数据完整性约束违反，请检查数据的唯一性要求");
	}

}
