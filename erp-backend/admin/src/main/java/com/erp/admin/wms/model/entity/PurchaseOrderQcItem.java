package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质检数据实体
 *
 * @author erp
 */
@Data
@TableName("wms_purchase_order_qc_item")
public class PurchaseOrderQcItem {

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
	 * SKU编码
	 */
	private String skuCode;

	/**
	 * 包装长度(cm)
	 */
	private BigDecimal lengthCm;

	/**
	 * 包装宽度(cm)
	 */
	private BigDecimal widthCm;

	/**
	 * 包装高度(cm)
	 */
	private BigDecimal heightCm;

	/**
	 * 毛重(KG)
	 */
	private BigDecimal grossWeightKg;

	/**
	 * 净重(KG)
	 */
	private BigDecimal netWeightKg;

	/**
	 * 质检报告文件ID（关联sys_file表）
	 */
	private Long qcFileId;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 创建人
	 */
	private Long createBy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createTime;

	/**
	 * 更新人
	 */
	private Long updateBy;

	/**
	 * 更新时间
	 */
	private LocalDateTime updateTime;

}
