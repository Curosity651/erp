package com.erp.admin.platform.yandex.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Yandex Market API 错误响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexErrorResponse {

	private String status;

	@JsonProperty("errors")
	private List<ErrorDetail> errors;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ErrorDetail {
		private String code;
		private String message;
	}

	/**
	 * 获取第一个错误码
	 */
	public String getFirstErrorCode() {
		if (errors != null && !errors.isEmpty()) {
			return errors.get(0).getCode();
		}
		return null;
	}

	/**
	 * 获取第一个错误消息
	 */
	public String getFirstErrorMessage() {
		if (errors != null && !errors.isEmpty()) {
			return errors.get(0).getMessage();
		}
		return null;
	}
}
