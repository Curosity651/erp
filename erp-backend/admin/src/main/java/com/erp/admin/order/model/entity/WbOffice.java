package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wildberries 专用 Office（/api/v3/offices）
 */
@Data
@TableName("wb_office")
@Schema(title = "WB Office")
public class WbOffice {

    @TableId
    @Schema(title = "主键")
    private Long id;

    @Schema(title = "店铺ID (关联 shop.id)")
    private Long shopId;

    @Schema(title = "officeId (WB id)")
    private Long officeId;

    @Schema(title = "名称 name")
    private String name;

    @Schema(title = "地址 address")
    private String address;

    @Schema(title = "城市 city")
    private String city;

    @Schema(title = "经度 longitude")
    private BigDecimal longitude;

    @Schema(title = "纬度 latitude")
    private BigDecimal latitude;

    @Schema(title = "货物类型 cargoType")
    private Integer cargoType;

    @Schema(title = "交付类型 deliveryType")
    private Integer deliveryType;

    @Schema(title = "联邦区 federalDistrict")
    private String federalDistrict;

    @Schema(title = "是否选中 selected")
    private Integer selected;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "记录创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "记录更新时间")
    private LocalDateTime updateTime;
}


