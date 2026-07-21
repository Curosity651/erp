package com.erp.admin.product.model.vo;

import java.math.BigDecimal;
import java.net.URL;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import com.erp.admin.product.excel.SafeUrlImageConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU导出对象
 *
 * @author ballcat 2025-08-02
 */
@ContentRowHeight(20)
@Data
@Schema(title = "SKU导出对象")
public class SkuExportVO {

	@ColumnWidth(15)
	@ExcelProperty(value = "图片", converter = SafeUrlImageConverter.class)
	private URL pic;

	@ColumnWidth(15)
	@ExcelProperty("类目")
	private String categoryName;

	@ColumnWidth(12)
	@ExcelProperty("SKU")
	private String skuCode;

	@ColumnWidth(12)
	@ExcelProperty("SPU")
	private String spuCode;

	@ColumnWidth(10)
	@ExcelProperty("序号")
	private Integer skuNo;

	@ColumnWidth(30)
	@ExcelProperty("配置")
	private String description;

	@ColumnWidth(10)
	@ExcelProperty("包装长")
	private BigDecimal packageLength;

	@ColumnWidth(10)
	@ExcelProperty("包装宽")
	private BigDecimal packageWidth;

	@ColumnWidth(10)
	@ExcelProperty("包装高")
	private BigDecimal packageHeight;

	@ColumnWidth(12)
	@ExcelProperty("包装单位")
	private String packageUnit;

	@ColumnWidth(10)
	@ExcelProperty("体积")
	private BigDecimal volume;

	@ColumnWidth(10)
	@ExcelProperty("毛重")
	private BigDecimal weight;

	@ColumnWidth(12)
	@ExcelProperty("重量单位")
	private String weightUnit;

	@ColumnWidth(10)
	@ExcelProperty("单价")
	private BigDecimal purchasePrice;

}
