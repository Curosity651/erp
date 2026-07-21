package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StocktakeMode {

	FULL("全仓盘点"),
	CYCLE("循环盘点"),
	SPECIAL("专项盘点");

	private final String description;
}

