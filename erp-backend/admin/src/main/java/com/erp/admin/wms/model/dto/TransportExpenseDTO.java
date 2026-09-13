package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class TransportExpenseDTO {
	@NotBlank(message = "费用类型不能为空")
	@Pattern(regexp = "FUEL|DRIVER_MONTHLY", message = "费用类型不正确")
	private String expenseType;
	private LocalDate expenseDate;
	@Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "月结月份格式应为YYYY-MM")
	private String settlementMonth;
	@Size(max = 128, message = "司机不能超过128个字符")
	private String driverName;
	@NotNull(message = "金额不能为空")
	@DecimalMin(value = "0", message = "金额不能小于0")
	private BigDecimal amount;
	@Size(max = 8, message = "币种不能超过8个字符")
	private String currency;
	@Size(max = 500, message = "备注不能超过500个字符")
	private String note;
}
