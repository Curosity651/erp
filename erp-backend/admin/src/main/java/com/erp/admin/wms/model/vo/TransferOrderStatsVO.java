package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 调拨单明细摘要VO（用于分页查询聚合）
 *
 * @author erp
 */
@Data
public class TransferOrderStatsVO {

	/**
	 * 调拨单ID
	 */
	private Long transferOrderId;

	/**
	 * SKU编码
	 */
	private String skuCode;

	/**
	 * 数量
	 */
	private Integer quantity;

}
