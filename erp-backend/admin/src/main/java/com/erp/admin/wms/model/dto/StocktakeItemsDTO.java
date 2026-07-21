package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 盘点录入数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点录入数据传输对象")
public class StocktakeItemsDTO {

	@NotNull(message = "盘点单ID不能为空")
	@Schema(title = "盘点单ID")
	private Long stocktakeId;

	@Valid
	@NotEmpty(message = "盘点明细不能为空")
	@Schema(title = "盘点明细列表")
	private List<StocktakeItemInputDTO> items;

}
