package com.erp.admin.statistics.model.stat;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Article销量统计结果
 *
 * @author erp
 */
@Data
public class ArticleSalesStat {

	/**
	 * Article编码
	 */
	private String article;

	/**
	 * 平台
	 */
	private String platform;

	/**
	 * 销量
	 */
	private Long quantity;

	/**
	 * 总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 是否已映射内部SKU
	 */
	private Boolean isMapped;

}
