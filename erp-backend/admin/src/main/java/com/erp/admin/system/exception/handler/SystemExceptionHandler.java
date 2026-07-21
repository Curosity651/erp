package com.erp.admin.system.exception.handler;

import com.erp.admin.system.exception.SystemBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 系统相关异常处理器
 *
 * @author ballcat
 */
@Slf4j
@RestControllerAdvice
public class SystemExceptionHandler {

	/**
	 * 处理系统业务异常
	 */
	@ExceptionHandler(SystemBusinessException.class)
	public ApiResult<Void> handleSystemBusinessException(SystemBusinessException e) {
		log.warn("系统业务异常: code={}, message={}", e.getCode(), e.getMessage());
		return ApiResult.failed(e.getCode(), e.getMessage());
	}

	/**
	 * 处理非法状态异常
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ApiResult<Void> handleIllegalStateException(IllegalStateException e) {
		log.warn("非法状态异常: message={}", e.getMessage());
		return ApiResult.failed(400, e.getMessage());
	}

}
