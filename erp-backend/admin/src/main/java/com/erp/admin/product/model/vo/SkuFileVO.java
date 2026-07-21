package com.erp.admin.product.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU文件VO
 *
 * @author system
 */
@Data
@Schema(title = "SKU文件VO")
public class SkuFileVO {

	/**
	 * 主键ID
	 */
	@Schema(title = "主键ID")
	private Long id;

	/**
	 * 文件类型
	 */
	@Schema(title = "文件类型", description = "actual_image、platform_image、manual、label等")
	private String fileType;

	/**
	 * OSS对象键
	 */
	@Schema(title = "OSS对象键", description = "用于构建文件访问URL")
	private String objectKey;

	/**
	 * 文件访问URL
	 */
	@Schema(title = "文件访问URL", description = "前端通过objectKey构建的完整URL")
	private String fileUrl;

	/**
	 * 排序顺序
	 */
	@Schema(title = "排序顺序", description = "数值越小越靠前")
	private Integer sortOrder;

}
