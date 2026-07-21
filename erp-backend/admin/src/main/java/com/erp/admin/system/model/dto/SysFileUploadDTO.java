package com.erp.admin.system.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 系统文件上传数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "系统文件上传数据传输对象")
public class SysFileUploadDTO {

	@NotBlank(message = "桶别名不能为空")
	@Size(max = 50, message = "桶别名长度不能超过50个字符")
	@Schema(title = "桶别名")
	private String bucketKey;

	@NotBlank(message = "OSS对象键不能为空")
	@Size(max = 500, message = "OSS对象键长度不能超过500个字符")
	@Schema(title = "OSS对象键")
	private String objectKey;

	@NotBlank(message = "原始文件名不能为空")
	@Size(max = 200, message = "原始文件名长度不能超过200个字符")
	@Schema(title = "原始文件名")
	private String fileName;

	@NotNull(message = "文件大小不能为空")
	@Schema(title = "文件大小(字节)")
	private Long fileSize;

	@NotBlank(message = "MIME类型不能为空")
	@Size(max = 100, message = "MIME类型长度不能超过100个字符")
	@Schema(title = "MIME类型")
	private String contentType;

}
