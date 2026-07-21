package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购单详情视图对象
 * <p>
 * 包含完整数据：付款凭证、合同文件、质检数据、其他附件
 *
 * @author erp
 */
@Data
@Schema(title = "采购单详情视图对象")
public class PurchaseOrderDetailVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "采购单号（合同编号）")
	private String orderNo;

	@Schema(title = "供应商ID")
	private Long supplierId;

	@Schema(title = "供应商名称")
	private String supplierName;

	@Schema(title = "下单日期")
	private LocalDate orderDate;

	@Schema(title = "预计交货日期")
	private LocalDate expectedDeliveryDate;

	@Schema(title = "实际交货日期")
	private LocalDate actualDeliveryDate;

	@Schema(title = "合同总金额")
	private BigDecimal totalAmount;

	@Schema(title = "币种")
	private String currencyCode;

	@Schema(title = "是否含税: 1-含税 / 0-不含税")
	private Integer taxIncluded;

	@Schema(title = "首付款比例")
	private BigDecimal prepayRatio;

	@Schema(title = "首付款金额")
	private BigDecimal prepayAmount;

	@Schema(title = "尾款账期(天)")
	private Integer balancePaymentDays;

	@Schema(title = "首付款状态: 0-未付 / 1-已付")
	private Integer prepayStatus;

	@Schema(title = "首付款时间")
	private LocalDateTime prepayTime;

	@Schema(title = "尾款状态: 0-未付 / 1-已付")
	private Integer balanceStatus;

	@Schema(title = "尾款时间")
	private LocalDateTime balancePayTime;

	@Schema(title = "采购单状态")
	private String orderStatus;

	@Schema(title = "采购单状态描述")
	private String orderStatusDesc;

	@Schema(title = "发货状态")
	private String shippingStatus;

	@Schema(title = "发货状态描述")
	private String shippingStatusDesc;

	@Schema(title = "入库状态")
	private String receivingStatus;

	@Schema(title = "入库状态描述")
	private String receivingStatusDesc;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "创建人")
	private Long createBy;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新人")
	private Long updateBy;

	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@Schema(title = "采购明细列表")
	private List<PurchaseOrderItemVO> items;

	@Schema(title = "总采购数量")
	private Integer totalQuantity;

	@Schema(title = "总已发货数量")
	private Integer totalShippedQuantity;

	@Schema(title = "总已入库数量")
	private Integer totalReceivedQuantity;

	// ========== 新增字段：付款凭证信息 ==========

	@Schema(title = "首付款凭证文件信息")
	private FileInfoVO prepayVoucherFile;

	@Schema(title = "尾款凭证文件信息")
	private FileInfoVO balanceVoucherFile;

	// ========== 新增字段：合同文件信息 ==========

	@Schema(title = "合同文件信息")
	private FileInfoVO contractFile;

	// ========== 新增字段：质检数据 ==========

	@Schema(title = "质检数据列表")
	private List<PurchaseOrderQcItemVO> qcItems;

	// ========== 新增字段：其他附件 ==========

	@Schema(title = "其他附件列表")
	private List<FileInfoVO> otherFiles;

	// ========== 新增字段：SKU展示信息 ==========

	@Schema(title = "SKU展示信息映射表", description = "key为skuCode，value为SKU展示信息")
	private Map<String, SkuBriefVO> skuBriefMap;

}
