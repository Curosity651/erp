package com.erp.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统文件实体
 *
 * @author erp
 */
@Data
@TableName("sys_file")
@Schema(title = "系统文件实体")
public class SysFile {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "OSS桶名称")
	private String bucketName;

	@Schema(title = "OSS桶别名")
	private String bucketKey;

	@Schema(title = "OSS对象键")
	private String objectKey;

	@Schema(title = "原始文件名")
	private String fileName;

	@Schema(title = "文件大小(字节)")
	private Long fileSize;

	@Schema(title = "MIME类型")
	private String contentType;

	@Schema(title = "上传人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "上传时间")
	private LocalDateTime createTime;

}
