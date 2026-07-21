package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 物流单关联采购单VO（用于分页查询聚合）
 *
 * @author erp
 */
@Data
public class ShippingOrderStatsVO {

	/**
	 * 物流单ID
	 */
	private Long shippingOrderId;

	/**
	 * 采购单号
	 */
	private String purchaseOrderNo;

}
