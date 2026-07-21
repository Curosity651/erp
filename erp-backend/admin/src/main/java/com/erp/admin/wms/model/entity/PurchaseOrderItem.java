package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_purchase_order_item")
@Schema(title = "采购单明细实体")
public class PurchaseOrderItem {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "采购单ID")
	private Long purchaseOrderId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "采购数量")
	private Integer quantity;

	@Schema(title = "单价")
	private BigDecimal unitPrice;

	@Schema(title = "金额")
	private BigDecimal amount;

	@Schema(title = "已发货数量")
	private Integer shippedQuantity;

	@Schema(title = "已入库数量")
	private Integer receivedQuantity;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
