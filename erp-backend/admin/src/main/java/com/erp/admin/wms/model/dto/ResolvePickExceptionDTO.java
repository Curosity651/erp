package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ResolvePickExceptionDTO {

	@NotNull(message = "拣货任务ID不能为空")
	private Long taskId;

	@NotBlank(message = "处理方式不能为空")
	private String action;

	private Long replacementInventoryId;

	private String remark;

}

