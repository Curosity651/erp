package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_stocktake_order_item")
@Schema(title = "盘点单明细实体")
public class StocktakeOrderItem {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "盘点单ID")
	private Long stocktakeOrderId;

	@Schema(title = "库位任务ID")
	private Long locationTaskId;

	@Schema(title = "物理库存批次ID，账外新增为空")
	private Long physicalInventoryId;

	@Schema(title = "货主（货物归属），仓库级库存按货主隔离；全量盘点由库存行自动带入，追加SKU时指定")
	private Long erpTenantId;

	@Schema(title = "WMS服务商ID")
	private Long wmsTenantId;

	@Schema(title = "SKU编码")
	private String skuCode;

	private Long zoneId;

	private String locationCode;

	private String quality;

	private Integer allocatable;

	private java.time.LocalDate inboundDate;

	private Integer pickOrder;

	@Schema(title = "系统数量")
	private Integer systemQuantity;

	private Integer reservedQuantity;

	@Schema(title = "实盘数量")
	private Integer actualQuantity;

	@Schema(title = "差异数量（实盘数量 - 系统数量）")
	private Integer diffQuantity;

	@Schema(title = "盘点状态: PENDING-未盘 / COUNTED-已盘")
	private String stocktakeStatus;

	private String reviewStatus;

	@Schema(title = "SKU来源: EXISTING-库内 / ADDED-追加")
	private String sourceType;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
