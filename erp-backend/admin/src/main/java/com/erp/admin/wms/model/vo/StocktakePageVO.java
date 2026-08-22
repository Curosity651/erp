package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点单分页视图对象")
public class StocktakePageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "盘点单号")
	private String stocktakeNo;

	@Schema(title = "盘点仓库ID")
	private Long warehouseId;

	@Schema(title = "盘点仓库名称")
	private String warehouseName;

	@Schema(title = "盘点日期")
	private LocalDate stocktakeDate;

	@Schema(title = "盘点范围: ALL-全部SKU / PARTIAL-指定SKU")
	private String stocktakeScope;

	@Schema(title = "盘点模式: FULL/CYCLE/SPECIAL")
	private String stocktakeMode;

	private String freezeMode;

	private Integer blindCount;

	private Integer locationCount;

	private Integer completedLocationCount;

	@Schema(title = "状态: COUNTING-盘点中 / CONFIRMED-已确认 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "SKU数量")
	private Integer skuCount;

	@Schema(title = "差异数量（有差异的SKU数）")
	private Integer diffCount;

	@Schema(title = "已盘点数量")
	private Integer countedCount;

	@Schema(title = "确认时间")
	private LocalDateTime confirmTime;

	@Schema(title = "确认人名称")
	private String confirmByName;

	@Schema(title = "盘点操作人，多个姓名以顿号分隔")
	private String operatorNames;

	@Schema(title = "创建人名称")
	private String createByName;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
