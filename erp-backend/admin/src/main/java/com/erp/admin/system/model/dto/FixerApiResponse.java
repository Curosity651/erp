package com.erp.admin.system.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Fixer.io API响应DTO
 *
 * @author system
 */
@Data
public class FixerApiResponse {

	/**
	 * 请求是否成功
	 */
	private Boolean success;

	private Boolean historical;

	/**
	 * 时间戳(秒级)
	 */
	private Long timestamp;

	/**
	 * 基准货币(Fixer.io固定返回EUR)
	 */
	private String base;

	/**
	 * 汇率日期
	 */
	private String date;

	/**
	 * 汇率数据映射,key为货币代码,value为汇率
	 */
	private Map<String, BigDecimal> rates;

	/**
	 * 错误信息(当success为false时)
	 */
	private Error error;

	@Data
	public static class Error {

		/**
		 * 错误代码
		 */
		private Integer code;

		/**
		 * 错误信息
		 */
		private String info;

	}

}
