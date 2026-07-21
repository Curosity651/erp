package com.erp.admin.wms.model.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物流单导出视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单导出视图对象")
public class ShippingOrderExportVO {

	@ExcelIgnore
	@Schema(title = "主键ID")
	private Long id;

	@ExcelIgnore
	@Schema(title = "物流商ID")
	private Long providerId;

	@ColumnWidth(15)
	@ExcelProperty("物流单号")
	@Schema(title = "物流单号")
	private String shippingNo;

	@ColumnWidth(15)
	@ExcelProperty("物流商")
	@Schema(title = "物流商名称")
	private String providerName;

	@ColumnWidth(12)
	@ExcelProperty("发货日期")
	@Schema(title = "发货日期")
	private LocalDate shippingDate;

	@ColumnWidth(12)
	@ExcelProperty("预计到货日期")
	@Schema(title = "预计到货日期")
	private LocalDate estimatedArrivalDate;

	@ColumnWidth(10)
	@ExcelProperty("物流方式")
	@Schema(title = "物流方式")
	private String shippingMethodDesc;

	@ColumnWidth(10)
	@ExcelProperty("物流线路")
	@Schema(title = "物流线路")
	private String shippingRouteDesc;

	@ColumnWidth(10)
	@ExcelProperty("发货件数")
	@Schema(title = "发货件数")
	private Integer packageCount;

	@ColumnWidth(12)
	@ExcelProperty("总重量(KG)")
	@Schema(title = "总重量(KG)")
	private BigDecimal totalWeight;

	@ColumnWidth(15)
	@ExcelProperty("物流总额(USD)")
	@Schema(title = "物流总金额(USD)")
	private BigDecimal totalAmount;

	@ColumnWidth(15)
	@ExcelProperty("物流总额(CNY)")
	@Schema(title = "物流总金额(CNY)")
	private BigDecimal totalAmountCny;

	@ColumnWidth(10)
	@ExcelProperty("付款状态")
	@Schema(title = "付款状态")
	private String paymentStatusDesc;

	@ColumnWidth(10)
	@ExcelProperty("物流状态")
	@Schema(title = "物流单状态")
	private String shippingStatusDesc;

	@ColumnWidth(30)
	@ExcelProperty("关联采购单")
	@Schema(title = "关联采购单号")
	private String purchaseOrderNos;

	@ColumnWidth(20)
	@ExcelProperty("创建时间")
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
