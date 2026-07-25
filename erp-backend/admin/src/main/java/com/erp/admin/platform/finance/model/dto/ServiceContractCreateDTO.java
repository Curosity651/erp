package com.erp.admin.platform.finance.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ServiceContractCreateDTO {
    @NotBlank
    private String contractNo;
    @NotNull
    private Long wmsTenantId;
    @NotNull
    private Long warehouseId;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private Integer rackUnitCount;
    private BigDecimal monthlyRentPerUnit;
    private BigDecimal warehouseDeposit;
    private BigDecimal subscriptionTotal;
    private BigDecimal refundableRate;
    private String contractFileUrl;
    private String remark;
    private List<String> rackNos;
}

