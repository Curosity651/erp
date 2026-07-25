package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PickExceptionDTO {

	@NotNull(message = "拣货任务ID不能为空")
	private Long taskId;

	@NotNull(message = "拣货明细ID不能为空")
	private Long lineId;

	@NotNull(message = "缺货数量不能为空")
	@Min(value = 1, message = "缺货数量必须大于0")
	private Integer shortageQty;

	@NotBlank(message = "异常原因不能为空")
	private String reason;

}

