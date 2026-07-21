package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 调整单统计信息VO（用于分页查询聚合）
 *
 * @author erp
 */
@Data
public class AdjustmentStatsVO {

	/**
	 * 调整单ID
	 */
	private Long adjustmentOrderId;

	/**
	 * SKU数量
	 */
	private Integer skuCount;

	/**
	 * 调整总数量
	 */
	private Integer totalQuantity;

}
