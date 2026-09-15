package com.erp.admin.wms.model.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class OutboundPickReviewConfirmDTO {
	@NotNull(message = "作业日期不能为空")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate workDate;
	private Long warehouseId;
	private String remark;
}
