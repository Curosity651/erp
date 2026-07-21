package com.erp.admin.product.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU文件DTO
 *
 * @author system
 */
@Data
@Schema(title = "SKU文件DTO")
public class SkuFileDTO {

	/**
	 * 文件类型
	 */
	@NotBlank(message = "文件类型不能为空")
	@Size(max = 50, message = "文件类型长度不能超过50个字符")
	@Schema(title = "文件类型",
			description = "actual_image、platform_image、manual、label、installation_video、quality_report、specification、demo_video、packaging_info、safety_info")
	private String fileType;

	/**
	 * OSS对象键
	 */
	@NotBlank(message = "OSS对象键不能为空")
	@Size(max = 500, message = "OSS对象键长度不能超过500个字符")
	@Schema(title = "OSS对象键", description = "用于构建文件访问URL")
	private String objectKey;

}
