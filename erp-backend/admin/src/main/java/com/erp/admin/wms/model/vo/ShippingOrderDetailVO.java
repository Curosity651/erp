package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物流单详情视图对象
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "物流单详情视图对象")
public class ShippingOrderDetailVO extends ShippingOrderPageVO {

	@Schema(title = "预计运输时效(天)")
	private Integer estimatedDays;

	@Schema(title = "物流单价(USD/kg，灰关)")
	private BigDecimal unitPrice;

	@Schema(title = "运输费用(USD，白关)")
	private BigDecimal shippingFee;

	@Schema(title = "杂费(USD，白关)")
	private BigDecimal miscFee;

	@Schema(title = "物流总金额(CNY)")
	private BigDecimal totalAmountCny;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "关联库存过账单ID")
	private Long stockPostingId;

	@Schema(title = "货物明细列表")
	private List<ShippingOrderItemVO> items;

	@Schema(title = "付款凭证文件ID")
	private Long paymentVoucherFileId;

	@Schema(title = "付款凭证文件名")
	private String paymentVoucherFileName;

	@Schema(title = "付款凭证文件大小")
	private Long paymentVoucherFileSize;

	// 关联入库单列表（预留，等入库单功能开发后实现）
	// private List<InboundOrderVO> inboundOrders;

}
