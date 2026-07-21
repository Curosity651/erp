package com.erp.admin.wms.model.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购单导出视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购单导出视图对象")
public class PurchaseOrderExportVO {

	@ExcelIgnore
	@Schema(title = "主键ID")
	private Long id;

	@ExcelProperty("采购单号")
	@ColumnWidth(20)
	@Schema(title = "采购单号")
	private String orderNo;

	@ExcelProperty("供应商")
	@ColumnWidth(20)
	@Schema(title = "供应商名称")
	private String supplierName;

	@ExcelProperty("订单状态")
	@ColumnWidth(12)
	@Schema(title = "订单状态")
	private String orderStatusDesc;

	@ExcelProperty("下单日期")
	@ColumnWidth(12)
	@Schema(title = "下单日期")
	private String orderDate;

	@ExcelProperty("预计交货日期")
	@ColumnWidth(14)
	@Schema(title = "预计交货日期")
	private String expectedDeliveryDate;

	@ExcelProperty("实际交货日期")
	@ColumnWidth(14)
	@Schema(title = "实际交货日期")
	private String actualDeliveryDate;

	@ExcelProperty("币种")
	@ColumnWidth(8)
	@Schema(title = "币种")
	private String currencyCode;

	@ExcelProperty("合同总金额")
	@ColumnWidth(14)
	@Schema(title = "合同总金额")
	private BigDecimal totalAmount;

	@ExcelProperty("首付款状态")
	@ColumnWidth(12)
	@Schema(title = "首付款状态")
	private String prepayStatusDesc;

	@ExcelProperty("尾款状态")
	@ColumnWidth(12)
	@Schema(title = "尾款状态")
	private String balanceStatusDesc;

	@ExcelProperty("SKU数量")
	@ColumnWidth(10)
	@Schema(title = "SKU数量")
	private Integer skuCount;

	@ExcelProperty("总采购数量")
	@ColumnWidth(12)
	@Schema(title = "总采购数量")
	private Integer totalQuantity;

	@ExcelProperty("已发货数量")
	@ColumnWidth(12)
	@Schema(title = "已发货数量")
	private Integer totalShippedQuantity;

	@ExcelProperty("已入库数量")
	@ColumnWidth(12)
	@Schema(title = "已入库数量")
	private Integer totalReceivedQuantity;

	@ExcelProperty("创建时间")
	@ColumnWidth(18)
	@Schema(title = "创建时间")
	private String createTime;

}
