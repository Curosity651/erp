package com.erp.admin.product.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU映射数量统计DTO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU映射数量统计DTO")
public class SkuMappingCountDTO {

	/**
	 * SKU编码
	 */
	@Schema(title = "SKU编码")
	private String skuCode;

	/**
	 * 映射数量
	 */
	@Schema(title = "映射数量")
	private Integer count;

}
