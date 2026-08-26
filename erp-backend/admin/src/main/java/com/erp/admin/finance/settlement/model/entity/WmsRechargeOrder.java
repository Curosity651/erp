package com.erp.admin.finance.settlement.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_recharge_order")
public class WmsRechargeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String rechargeNo;
    private String accountScope;
    private Long wmsTenantId;
    private Long erpTenantId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime paymentTime;
    private Long voucherFileId;
    private String status;
    private Long applicantId;
    private String applicantName;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String rejectReason;
    private Long reverseOrderId;
    private String reverseReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
