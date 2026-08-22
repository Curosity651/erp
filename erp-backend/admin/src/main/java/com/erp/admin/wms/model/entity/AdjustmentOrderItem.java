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
 * 库存调整单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_adjustment_order_item")
@Schema(title = "库存调整单明细实体")
public class AdjustmentOrderItem {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调整单ID")
	private Long adjustmentOrderId;

	@Schema(title = "货主（货物归属），仓库级库存按货主隔离，建单时按行指定")
	private Long erpTenantId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "目标批次ID（wms_physical_inventory.id）——报废锁定到具体批次/库位")
	private Long physicalInventoryId;

	@Schema(title = "逻辑库位库存ID")
	private Long sourceInventoryId;

	@Schema(title = "库位编码（冗余展示，取自批次）")
	private String locationCode;

	@Schema(title = "品质 GOOD/DAMAGED（冗余展示，取自批次）")
	private String quality;

	@Schema(title = "调整数量（始终为正数）")
	private Integer quantity;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
