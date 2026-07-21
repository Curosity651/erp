package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购单附件类型枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum PurchaseOrderFileType {

	CONTRACT("合同"),
	QUALITY_REPORT("质检报告"),
	PREPAY_VOUCHER("首付款凭证"),
	BALANCE_VOUCHER("尾款凭证"),
	OTHER("其他");

	private final String description;

}
