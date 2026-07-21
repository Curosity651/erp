package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点进度视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点进度视图对象")
public class StocktakeProgressVO {

	@Schema(title = "总SKU数")
	private Integer totalCount;

	@Schema(title = "已盘点数")
	private Integer countedCount;

	@Schema(title = "未盘点数")
	private Integer pendingCount;

	@Schema(title = "进度百分比")
	private BigDecimal progressPercent;

	@Schema(title = "盘盈项数")
	private Integer profitCount;

	@Schema(title = "盘盈总数量")
	private Integer profitQuantity;

	@Schema(title = "盘亏项数")
	private Integer lossCount;

	@Schema(title = "盘亏总数量")
	private Integer lossQuantity;

	@Schema(title = "净差异数量")
	private Integer netDiff;

}
