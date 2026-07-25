package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PickLineScanDTO {

	@NotNull(message = "拣货任务ID不能为空")
	private Long taskId;

	@NotNull(message = "拣货明细ID不能为空")
	private Long lineId;

	@NotBlank(message = "扫描码不能为空")
	private String scanCode;

	private String locationScanCode;

	@NotNull(message = "实拣数量不能为空")
	@Min(value = 1, message = "实拣数量必须大于0")
	private Integer quantity;

	private Boolean manual;

}
