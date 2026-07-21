package com.erp.admin.order.service.common.model;

import java.util.List;

import lombok.Data;

/**
 * 确认发货结果 DTO（跨平台通用）
 */
@Data
public class ConfirmResult {

	private List<Item> items;

	@Data
	public static class Item {

		private Long orderId;

		private boolean success;

		private String message;

		/** WB 特有字段：supply 批次号 */
		private String supplyId;

	}

}
