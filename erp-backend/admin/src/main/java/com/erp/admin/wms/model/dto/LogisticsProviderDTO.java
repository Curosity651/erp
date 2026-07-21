package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 物流商数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流商数据传输对象")
public class LogisticsProviderDTO {

	@Schema(title = "主键ID (编辑时必填)")
	private Long id;

	@NotBlank(message = "物流商编码不能为空")
	@Schema(title = "物流商编码")
	private String providerCode;

	@NotBlank(message = "物流商名称不能为空")
	@Schema(title = "物流商名称")
	private String providerName;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "联系邮箱")
	private String contactEmail;

	@NotNull(message = "状态不能为空")
	@Schema(title = "状态: 1-启用 / 0-停用")
	private Integer status;

	@Schema(title = "备注")
	private String remark;

}
