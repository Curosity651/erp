package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购单分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购单分页视图对象")
public class PurchaseOrderPageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "采购单号（合同编号）")
	private String orderNo;

	@Schema(title = "供应商ID")
	private Long supplierId;

	@Schema(title = "供应商编码")
	private String supplierCode;

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

	@Schema(title = "首付款状态: 0-未付 / 1-已付")
	private Integer prepayStatus;

	@Schema(title = "尾款状态: 0-未付 / 1-已付")
	private Integer balanceStatus;

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

	@Schema(title = "SKU种类数")
	private Integer skuCount;

	@Schema(title = "总采购数量")
	private Integer totalQuantity;

	@Schema(title = "总已发货数量")
	private Integer totalShippedQuantity;

	@Schema(title = "总已入库数量")
	private Integer totalReceivedQuantity;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
