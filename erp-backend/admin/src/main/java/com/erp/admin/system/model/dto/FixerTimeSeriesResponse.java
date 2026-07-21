package com.erp.admin.system.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Fixer.io Time-Series API响应DTO
 *
 * @author system
 */
@Data
public class FixerTimeSeriesResponse {

	/**
	 * 请求是否成功
	 */
	private Boolean success;

	/**
	 * 是否为时间序列数据
	 */
	private Boolean timeseries;

	/**
	 * 开始日期
	 */
	private String startDate;

	/**
	 * 结束日期
	 */
	private String endDate;

	/**
	 * 基准货币(Fixer.io固定返回EUR)
	 */
	private String base;

	/**
	 * 时间序列汇率数据
	 * key: 日期字符串(YYYY-MM-DD)
	 * value: 该日期的汇率数据Map(货币代码 -> 汇率值)
	 */
	private Map<String, Map<String, BigDecimal>> rates;

	/**
	 * 错误信息(当success为false时)
	 */
	private FixerApiResponse.Error error;

}
