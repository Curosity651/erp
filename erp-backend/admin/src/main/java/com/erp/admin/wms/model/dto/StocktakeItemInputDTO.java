package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 盘点明细录入数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点明细录入数据传输对象")
public class StocktakeItemInputDTO {

	@NotNull(message = "明细ID不能为空")
	@Schema(title = "盘点明细ID")
	private Long itemId;

	@Min(value = 0, message = "实盘数量不能为负数")
	@Schema(title = "实盘数量（null表示清除）")
	private Integer actualQuantity;

}
