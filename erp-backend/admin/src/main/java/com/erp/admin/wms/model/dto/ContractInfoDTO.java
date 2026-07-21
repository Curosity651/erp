package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 合同信息DTO
 * <p>
 * 用于采购单编辑时提交合同文件信息
 *
 * @author erp
 */
@Data
@Schema(title = "合同信息DTO")
public class ContractInfoDTO {

	@Schema(title = "合同文件ID（关联sys_file表）")
	private Long contractFileId;

	@Size(max = 20, message = "操作类型长度不能超过20个字符")
	@Schema(title = "操作类型: UPLOAD-上传 / REPLACE-替换 / null-不变")
	private String action;

	/**
	 * 操作类型常量
	 */
	public static final String ACTION_UPLOAD = "UPLOAD";
	public static final String ACTION_REPLACE = "REPLACE";

}
