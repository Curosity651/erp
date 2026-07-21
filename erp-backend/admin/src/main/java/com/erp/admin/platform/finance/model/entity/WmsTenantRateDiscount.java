package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WMS服务商费率折扣（按百分比叠加基础费率）。
 *
 * @author erp
 */
@Data
@TableName("wms_tenant_rate_discount")
@Schema(title = "WMS服务商费率折扣")
public class WmsTenantRateDiscount {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "WMS服务商")
    private Long wmsTenantId;

    @Schema(title = "折扣百分比(如 -0.10=九折; 实收=基础×(1+discount))")
    private BigDecimal discountPct;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
