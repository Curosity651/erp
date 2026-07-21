package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存桶枚举
 */
@Getter
@AllArgsConstructor
public enum StockBucket {

	/** 可用库存（可售/可调拨），必须 ≥ 0（L-1：超卖硬守卫，过量出库/预占过账直接抛错阻断） */
	AVAILABLE("可用库存", false),
	/** 占用库存（待出库），必须 ≥ 0 */
	RESERVED("占用库存", false),
	/** 在途库存（运输中），必须 ≥ 0 */
	IN_TRANSIT("在途库存", false),
	/** 残品库存，必须 ≥ 0 */
	DAMAGED("残品库存", false),
	/** 报废记录，仅用于流水，不影响库存快照 */
	SCRAP("报废", false);

	private final String description;
	private final boolean allowNegative;

}
