package com.erp.admin.platform.finance.model.dto;

import com.erp.admin.platform.finance.model.enums.BillAdjustmentType;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class BillAdjustmentDTO {

    @NotNull
    private BillAdjustmentType adjustmentType;

    @NotBlank
    private String feeCode;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String sourceRef;

    @NotBlank
    private String remark;
}
