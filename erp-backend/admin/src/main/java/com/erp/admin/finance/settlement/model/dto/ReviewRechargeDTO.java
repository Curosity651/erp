package com.erp.admin.finance.settlement.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ReviewRechargeDTO {
    @NotNull
    private Boolean approved;

    @Size(max = 500)
    private String reason;
}
