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
 * 物流单仓库调整记录实体
 *
 * @author erp
 */
@Data
@TableName("wms_shipping_order_adjust")
@Schema(title = "物流单仓库调整记录")
public class ShippingOrderAdjust {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "调整单号")
    private String adjustNo;

    @Schema(title = "物流单ID")
    private Long shippingOrderId;

    @Schema(title = "原区域ID")
    private Long fromRegionId;

    @Schema(title = "新区域ID")
    private Long toRegionId;

    @Schema(title = "关联过账单ID")
    private Long stockPostingId;

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

}
