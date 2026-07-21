package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 物流单数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单数据传输对象")
public class ShippingOrderDTO {

	@Null(groups = CreateGroup.class, message = "新建时ID必须为空")
	@NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
	@Schema(title = "主键ID")
	private Long id;

	@NotBlank(groups = CreateGroup.class, message = "物流单号不能为空")
	@Size(max = 50, message = "物流单号长度不能超过50")
	@Schema(title = "物流单号")
	private String shippingNo;

	@NotNull(message = "物流商不能为空")
	@Schema(title = "物流商ID")
	private Long providerId;

	@NotNull(message = "发货日期不能为空")
	@Schema(title = "发货日期")
	private LocalDate shippingDate;

	@Schema(title = "预计到货日期")
	private LocalDate estimatedArrivalDate;

	@Schema(title = "预计运输时效(天)")
	private Integer estimatedDays;

	@NotBlank(message = "物流方式不能为空")
	@Schema(title = "物流方式: GRAY-灰关 / WHITE-白关")
	private String shippingMethod;

	@NotBlank(message = "物流线路不能为空")
	@Schema(title = "物流线路: EAST-东线 / WEST-西线 / RAIL-铁路")
	private String shippingRoute;

	@NotNull(message = "目标区域不能为空")
	@Schema(title = "目标区域ID")
	private Long targetRegionId;

	@NotNull(message = "总重量不能为空")
	@Schema(title = "总重量(KG)")
	private BigDecimal totalWeight;

	@Schema(title = "物流单价(USD/kg，灰关时必填)")
	private BigDecimal unitPrice;

	@Schema(title = "运输费用(USD，白关时必填)")
	private BigDecimal shippingFee;

	@Schema(title = "杂费(USD，白关时必填)")
	private BigDecimal miscFee;

	@Schema(title = "物流总金额(USD)")
	private BigDecimal totalAmount;

	@Schema(title = "物流总金额(CNY)")
	private BigDecimal totalAmountCny;

	@Size(max = 500, message = "备注长度不能超过500")
	@Schema(title = "备注")
	private String remark;

	@Valid
	@NotEmpty(groups = CreateGroup.class, message = "货物明细不能为空")
	@Schema(title = "货物明细列表")
	private List<ShippingOrderItemDTO> items;

	@Schema(title = "付款状态: 0-未付 / 1-已付")
	private Integer paymentStatus;

	@Schema(title = "付款凭证文件ID")
	private Long paymentVoucherFileId;

}
