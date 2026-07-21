package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购单数据传输对象
 * <p>
 * 支持完整数据提交，包括付款信息、合同、质检数据、其他附件
 *
 * @author erp
 */
@Data
@Schema(title = "采购单数据传输对象")
public class PurchaseOrderDTO {

	@Null(groups = CreateGroup.class, message = "新建时ID必须为空")
	@NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
	@Schema(title = "主键ID")
	private Long id;

	@NotBlank(groups = CreateGroup.class, message = "采购单号不能为空")
	@Size(max = 50, message = "采购单号长度不能超过50个字符")
	@Schema(title = "采购单号（合同编号）")
	private String orderNo;

	@NotNull(message = "供应商不能为空")
	@Schema(title = "供应商ID")
	private Long supplierId;

	@NotNull(message = "下单日期不能为空")
	@Schema(title = "下单日期")
	private LocalDate orderDate;

	@Schema(title = "预计交货日期")
	private LocalDate expectedDeliveryDate;

	@Schema(title = "实际交货日期")
	private LocalDate actualDeliveryDate;

	@Size(max = 10, message = "币种长度不能超过10个字符")
	@Schema(title = "币种", example = "CNY")
	private String currencyCode;

	@Schema(title = "是否含税: 1-含税 / 0-不含税")
	private Integer taxIncluded;

	@DecimalMin(value = "0", message = "首付款比例不能小于0")
	@DecimalMax(value = "100", message = "首付款比例不能大于100")
	@Schema(title = "首付款比例", example = "30")
	private BigDecimal prepayRatio;

	@Schema(title = "尾款账期(天)")
	private Integer balancePaymentDays;

	@Size(max = 500, message = "备注长度不能超过500个字符")
	@Schema(title = "备注")
	private String remark;

	@Valid
	@NotEmpty(groups = CreateGroup.class, message = "采购明细不能为空")
	@Schema(title = "采购明细列表")
	private List<PurchaseOrderItemDTO> items;

	// ========== 新增字段：付款信息 ==========

	@Valid
	@Schema(title = "付款信息")
	private PaymentInfoDTO paymentInfo;

	// ========== 新增字段：合同信息 ==========

	@Valid
	@Schema(title = "合同信息")
	private ContractInfoDTO contractInfo;

	// ========== 新增字段：质检数据 ==========

	@Valid
	@Schema(title = "质检数据")
	private QcDataDTO qcData;

	// ========== 新增字段：其他附件 ==========

	@Schema(title = "其他附件文件ID列表")
	private List<Long> otherFileIds;

}
