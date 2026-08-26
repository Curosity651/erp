package com.erp.admin.finance.settlement.model.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateRechargeDTO {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3,8}")
    private String currency;

    @NotNull
    private LocalDateTime paymentTime;

    @NotNull
    private Long voucherFileId;

    @Size(max = 500)
    private String remark;
}
