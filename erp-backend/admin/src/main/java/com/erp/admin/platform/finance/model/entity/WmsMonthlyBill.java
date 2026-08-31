package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度应收账单头（链路一 平台→WMS服务商）。
 *
 * @author erp
 */
@Data
@TableName("wms_monthly_bill")
@Schema(title = "月度应收账单")
public class WmsMonthlyBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "账期 YYYY-MM")
    private String billMonth;

    @Schema(title = "WMS服务商")
    private Long wmsTenantId;

    private BigDecimal rackFee;

    private BigDecimal inboundFee;

    private BigDecimal outboundFee;

    private BigDecimal deliveryFee;

    private BigDecimal returnFee;

    private BigDecimal inspectionFee;

    private BigDecimal driverFee;

    private BigDecimal totalAmount;

    @Schema(title = "账单币种")
    private String currency;

    @Schema(title = "DRAFT/CONFIRMED/PAID/DISPUTED")
    private String status;

    private LocalDateTime confirmedTime;

    private Long reviewerId;

    private String reviewerName;

    private LocalDateTime paidTime;

    @Schema(title = "付款凭证文件ID")
    private Long paymentVoucherFileId;

    private String remark;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Long deleted;

}
