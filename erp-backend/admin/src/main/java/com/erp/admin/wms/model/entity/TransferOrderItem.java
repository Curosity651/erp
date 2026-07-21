package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调拨单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_transfer_order_item")
@Schema(title = "调拨单明细实体")
public class TransferOrderItem {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调拨单ID")
	private Long transferOrderId;

	@Schema(title = "货主（货物归属），仓库级库存按货主隔离，建单时按行指定")
	private Long erpTenantId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "源批次ID(A仓，发出时扣此批次)")
	private Long sourcePhysicalInventoryId;

	@Schema(title = "目标库位编码(B仓，须落在该货主服务商租用排)")
	private String targetLocationCode;

	@Schema(title = "调拨数量")
	private Integer quantity;

	@Schema(title = "实际入库数量")
	private Integer receivedQuantity;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "是否删除")
	private Integer deleted;

}
