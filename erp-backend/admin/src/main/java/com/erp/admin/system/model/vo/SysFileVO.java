package com.erp.admin.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统文件视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "系统文件视图对象")
public class SysFileVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "OSS对象键")
	private String objectKey;

	@Schema(title = "原始文件名")
	private String fileName;

	@Schema(title = "文件大小(字节)")
	private Long fileSize;

	@Schema(title = "MIME类型")
	private String contentType;

	@Schema(title = "上传时间")
	private LocalDateTime createTime;

	@Schema(title = "访问URL（公有桶直接返回，私有桶返回签名URL）")
	private String url;

}
