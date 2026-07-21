package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PalletCapacityDTO {
    @NotNull
    private Long palletId;
    @NotNull
    @Min(1)
    @Max(100)
    private BigDecimal capacityPercent;
    private BigDecimal actualWeightKg;
    private Boolean markFull;
    private String remark;
}
