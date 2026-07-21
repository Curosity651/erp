package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 物流单SKU种类数VO
 *
 * @author erp
 */
@Data
public class ShippingOrderSkuCountVO {

	/**
	 * 物流单ID
	 */
	private Long shippingOrderId;

	/**
	 * SKU种类数
	 */
	private Integer skuCount;

}
