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
 * 采购入库单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_purchase_inbound_order_item")
@Schema(title = "采购入库单明细实体")
public class PurchaseInboundOrderItem {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "入库单ID")
    private Long inboundOrderId;

    @Schema(title = "物流单明细ID")
    private Long shippingOrderItemId;

    @Schema(title = "采购单ID")
    private Long purchaseOrderId;

    @Schema(title = "采购单明细ID")
    private Long purchaseOrderItemId;

    @Schema(title = "SKU编码(冗余)")
    private String skuCode;

    @Schema(title = "应到数量")
    private Integer expectedQuantity;

    @Schema(title = "实到数量")
    private Integer actualQuantity;

    @Schema(title = "货品预判(仅自定义退货): GOOD良品 / PENDING待检 / DAMAGED不良品")
    private String expectedQuality;

    @Schema(title = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

}
