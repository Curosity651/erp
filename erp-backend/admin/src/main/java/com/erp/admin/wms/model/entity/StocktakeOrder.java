package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单实体
 *
 * @author erp
 */
@Data
@TableName("wms_stocktake_order")
@Schema(title = "盘点单实体")
public class StocktakeOrder {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "盘点单号（ST + 年月日 + 4位序号）")
	private String stocktakeNo;

	@Schema(title = "盘点仓库ID")
	private Long warehouseId;

	@Schema(title = "盘点日期")
	private LocalDate stocktakeDate;

	@Schema(title = "盘点范围: ALL-全部SKU / PARTIAL-指定SKU")
	private String stocktakeScope;

	@Schema(title = "盘点模式: FULL/CYCLE/SPECIAL")
	private String stocktakeMode;

	@Schema(title = "盘点范围JSON")
	private String scopeConfig;

	@Schema(title = "是否盲盘")
	private Integer blindCount;

	@Schema(title = "冻结方式: WAREHOUSE/LOCATION/ITEM")
	private String freezeMode;

	@Schema(title = "物理库存快照时间")
	private LocalDateTime snapshotTime;

	@Schema(title = "状态: COUNTING-盘点中 / CONFIRMED-已确认 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "确认时间")
	private LocalDateTime confirmTime;

	@Schema(title = "确认人")
	private Long confirmBy;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "逻辑删除标识")
	private Long deleted;

}
