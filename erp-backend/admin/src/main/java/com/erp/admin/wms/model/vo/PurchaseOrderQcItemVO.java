package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质检数据VO
 *
 * @author erp
 */
@Data
public class PurchaseOrderQcItemVO {

	/**
	 * 主键
	 */
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
	 * 体积(cm³) - 计算字段
	 */
	private BigDecimal volumeCm3;

	/**
	 * 毛重(KG)
	 */
	private BigDecimal grossWeightKg;

	/**
	 * 净重(KG)
	 */
	private BigDecimal netWeightKg;

	/**
	 * 质检报告文件ID
	 */
	private Long qcFileId;

	/**
	 * 质检报告文件名
	 */
	private String qcFileName;

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
