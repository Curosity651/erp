package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量查询库存DTO
 *
 * @author erp
 */
@Data
@Schema(title = "批量查询库存DTO")
public class BatchStockQueryDTO {

	@NotNull(message = "仓库ID不能为空")
	@Schema(title = "仓库ID")
	private Long warehouseId;

	@NotEmpty(message = "SKU编码列表不能为空")
	@Schema(title = "SKU编码列表")
	private List<String> skuCodes;

}
