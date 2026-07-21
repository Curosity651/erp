package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 库位调整单统计信息VO（用于分页聚合明细行数/总数量）。
 *
 * @author erp
 */
@Data
public class LocationTransferStatsVO {

	/** 调整单ID */
	private Long transferOrderId;

	/** 明细行数 */
	private Integer itemCount;

	/** 移动总数量 */
	private Integer totalQuantity;

}
