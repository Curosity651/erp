package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_contract_fund_ledger")
public class WmsContractFundLedger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private Long contractId;
    private Long wmsTenantId;
    private String fundComponent;
    private String transactionType;
    private String direction;
    private BigDecimal amount;
    private String currency;
    private String accountingMonth;
    private String status;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;
}

