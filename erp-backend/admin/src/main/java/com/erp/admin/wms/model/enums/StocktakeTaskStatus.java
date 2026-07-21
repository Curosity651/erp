package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StocktakeTaskStatus {

	PENDING("待盘"),
	COUNTING("盘点中"),
	COMPLETED("已完成"),
	RECOUNTING("复盘中"),
	REVIEWED("已复核");

	private final String description;
}

