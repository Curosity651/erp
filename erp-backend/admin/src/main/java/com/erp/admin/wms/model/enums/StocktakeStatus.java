package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点单状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum StocktakeStatus {
	/** 草稿 */
	DRAFT("草稿"),

	/** 已生成任务，待开始 */
	READY("待开始"),

	/**
	 * 盘点中
	 */
	COUNTING("盘点中"),

	/** 差异复核中 */
	REVIEWING("待复核"),

	/**
	 * 已确认
	 */
	CONFIRMED("已确认"),

	/**
	 * 已取消
	 */
	CANCELLED("已取消");

	private final String description;

}
