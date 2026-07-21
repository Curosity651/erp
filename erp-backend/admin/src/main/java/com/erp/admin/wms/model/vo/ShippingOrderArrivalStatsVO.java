package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物流单到货统计VO
 *
 * @author erp
 */
@Data
@Schema(title = "物流单到货统计VO")
public class ShippingOrderArrivalStatsVO {

	@Schema(title = "发货总数量")
	private Integer totalQuantity;

	@Schema(title = "已到货总数量")
	private Integer totalReceivedQuantity;

	/**
	 * 获取待到货数量
	 */
	public Integer getPendingQuantity() {
		int total = totalQuantity != null ? totalQuantity : 0;
		int received = totalReceivedQuantity != null ? totalReceivedQuantity : 0;
		return total - received;
	}

	/**
	 * 是否全部到货
	 */
	public boolean isAllArrived() {
		int total = totalQuantity != null ? totalQuantity : 0;
		int received = totalReceivedQuantity != null ? totalReceivedQuantity : 0;
		return total > 0 && received >= total;
	}

	/**
	 * 是否部分到货
	 */
	public boolean isPartialArrived() {
		int received = totalReceivedQuantity != null ? totalReceivedQuantity : 0;
		return received > 0 && !isAllArrived();
	}

}
