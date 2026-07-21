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
 * 销售出库单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_sales_outbound_order_item")
@Schema(title = "销售出库单明细实体")
public class SalesOutboundOrderItem {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @Schema(title = "关联电商订单ID")
    private Long erpOrderId;

    @Schema(title = "平台订单号(冗余)")
    private String platformOrderId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "出库数量")
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
