package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息VO
 * <p>
 * 用于返回采购单相关附件的文件信息
 *
 * @author erp
 */
@Data
@Schema(title = "文件信息VO")
public class FileInfoVO {

	@Schema(title = "采购单附件关联ID")
	private Long id;

	@Schema(title = "系统文件ID")
	private Long sysFileId;

	@Schema(title = "原始文件名")
	private String fileName;

	@Schema(title = "文件大小(字节)")
	private Long fileSize;

	@Schema(title = "MIME类型")
	private String contentType;

	@Schema(title = "文件类型")
	private String fileType;

	@Schema(title = "文件类型描述")
	private String fileTypeDesc;

	@Schema(title = "上传时间")
	private LocalDateTime createTime;

}
