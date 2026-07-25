package com.erp.admin.platform.finance.model.dto;

import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

@Data
public class ContractRefundDTO {
    @NotNull
    private Long contractId;
    @AssertTrue(message = "必须确认服务商无违约")
    private Boolean noDefault;
    @AssertTrue(message = "必须确认服务商无欠费")
    private Boolean noDebt;
    @AssertTrue(message = "必须确认仓库无遗留货物")
    private Boolean noRemainingGoods;
    private String remark;
}

