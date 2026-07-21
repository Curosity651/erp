package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购单附件关联实体
 *
 * @author erp
 */
@Data
@TableName("wms_purchase_order_file")
public class PurchaseOrderFile {

	/**
	 * 主键
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 采购单ID
	 */
	private Long purchaseOrderId;

	/**
	 * 系统文件ID（关联sys_file表）
	 */
	private Long sysFileId;

	/**
	 * 文件类型: CONTRACT-合同 / QUALITY_REPORT-质检报告 / PREPAY_VOUCHER-首付款凭证 / BALANCE_VOUCHER-尾款凭证 / OTHER-其他
	 */
	private String fileType;

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
