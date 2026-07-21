package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 盘点追加SKU请求
 *
 * @author erp
 */
@Data
@Schema(title = "盘点追加SKU请求")
public class StocktakeAddSkuDTO {

	@NotNull(message = "盘点单ID不能为空")
	@Schema(title = "盘点单ID")
	private Long stocktakeId;

	@NotNull(message = "货主不能为空")
	@Schema(title = "货主（货物归属），追加的这批SKU归属该货主")
	private Long erpTenantId;

	@NotEmpty(message = "SKU编码列表不能为空")
	@Schema(title = "SKU编码列表")
	private List<String> skuCodes;

}
