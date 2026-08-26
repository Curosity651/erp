package com.erp.admin.finance.settlement.model.qo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class FundLedgerExportQO {
    @NotNull(message = "请选择WMS服务商")
    private Long wmsTenantId;
    @NotBlank(message = "请选择币种")
    private String currency;
    @NotNull(message = "请选择开始日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @NotNull(message = "请选择结束日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
