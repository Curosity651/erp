package com.erp.admin.platform.finance.model.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Data
public class ManualBillingDTO {

    @NotNull
    private Long wmsTenantId;
    private Long erpTenantId;
    private Long warehouseId;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "账期格式必须为YYYY-MM")
    private String billMonth;

    @NotBlank
    private String feeCode;

    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal quantity;

    @DecimalMin(value = "0.01")
    private BigDecimal actualAmount;

    @NotBlank
    private String sourceRef;

    @NotBlank
    private String remark;
}

