package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购单附件VO
 *
 * @author erp
 */
@Data
public class PurchaseOrderFileVO {

	/**
	 * 主键
	 */
	private Long id;

	/**
	 * 采购单ID
	 */
	private Long purchaseOrderId;

	/**
	 * 系统文件ID
	 */
	private Long sysFileId;

	/**
	 * 文件类型
	 */
	private String fileType;

	/**
	 * 文件类型描述
	 */
	private String fileTypeDesc;

	/**
	 * 原始文件名
	 */
	private String fileName;

	/**
	 * 文件大小(字节)
	 */
	private Long fileSize;

	/**
	 * MIME类型
	 */
	private String contentType;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 上传人
	 */
	private Long createBy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createTime;

}
