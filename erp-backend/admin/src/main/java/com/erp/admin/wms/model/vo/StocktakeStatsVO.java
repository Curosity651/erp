package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 盘点单统计信息VO（用于分页查询聚合）
 *
 * @author erp
 */
@Data
public class StocktakeStatsVO {

	/**
	 * 盘点单ID
	 */
	private Long stocktakeOrderId;

	/**
	 * SKU数量
	 */
	private Integer skuCount;

	/**
	 * 差异数量（有差异的SKU数）
	 */
	private Integer diffCount;

	/**
	 * 已盘数量（已盘的SKU数）
	 */
	private Integer countedCount;


}
