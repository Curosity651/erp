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
 * 物流单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_shipping_order_item")
@Schema(title = "物流单明细实体")
public class ShippingOrderItem {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "物流单ID")
	private Long shippingOrderId;

	@Schema(title = "采购单ID")
	private Long purchaseOrderId;

	@Schema(title = "采购单明细ID")
	private Long purchaseOrderItemId;

	@Schema(title = "SKU编码(冗余)")
	private String skuCode;

	@Schema(title = "发货数量")
	private Integer quantity;

	@Schema(title = "已到货数量")
	private Integer receivedQuantity;

	@Schema(title = "Submitted inbound reserved quantity")
	private Integer inboundReservedQuantity;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
