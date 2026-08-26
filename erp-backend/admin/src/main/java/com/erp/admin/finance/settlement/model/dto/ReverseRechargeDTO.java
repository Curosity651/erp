package com.erp.admin.finance.settlement.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ReverseRechargeDTO {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
