package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点明细SKU来源
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum StocktakeItemSource {

	/**
	 * 仓库库存记录中已存在的SKU
	 */
	EXISTING("库内"),

	/**
	 * 盘点时追加的仓库外SKU
	 */
	ADDED("追加");

	private final String description;

}
