package com.erp.admin.wms.model.param;

import lombok.Builder;
import lombok.Data;

/**
 * 到货数量更新参数
 *
 * @author erp
 */
@Data
@Builder
public class ReceivedQuantityUpdateParam {

	/**
	 * 物流单明细ID
	 */
	private Long shippingOrderItemId;

	/**
	 * 增加的到货数量
	 */
	private Integer quantity;

}
