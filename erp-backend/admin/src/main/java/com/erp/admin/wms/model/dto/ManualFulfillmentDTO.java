package com.erp.admin.wms.model.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ManualFulfillmentDTO {
	private Long id;
	@NotNull(message = "仓库不能为空")
	private Long warehouseId;
	private String recipientName;
	private String recipientPhone;
	private String recipientAddress;
	@NotEmpty(message = "商品不能为空")
	@Valid
	private List<Item> items;

	@Data
	public static class Item {
		@NotBlank(message = "SKU不能为空")
		private String skuCode;
		@NotNull(message = "数量不能为空")
		@Min(value = 1, message = "数量必须大于0")
		private Integer quantity;
	}
}
