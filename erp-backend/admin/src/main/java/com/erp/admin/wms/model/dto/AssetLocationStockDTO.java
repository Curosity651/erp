package com.erp.admin.wms.model.dto;

import lombok.Data;

/** Logical-location inventory buckets used by owner asset valuation. */
@Data
public class AssetLocationStockDTO {

	private String skuCode;

	private Integer availableQuantity;

	private Integer reservedQuantity;

	private Integer damagedQuantity;

}
