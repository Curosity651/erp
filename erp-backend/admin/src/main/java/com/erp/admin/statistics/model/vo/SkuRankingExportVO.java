package com.erp.admin.statistics.model.vo;

import java.math.BigDecimal;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU排名导出VO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU排名导出")
public class SkuRankingExportVO {

	/**
	 * 排名
	 */
	@ColumnWidth(8)
	@ExcelProperty("排名")
	private Integer rank;

	/**
	 * SKU编码
	 */
	@ColumnWidth(15)
	@ExcelProperty("SKU编码")
	private String sku;

	/**
	 * 映射状态
	 */
	@ColumnWidth(10)
	@ExcelProperty("映射状态")
	private String mappedStatus;

	/**
	 * 产品名称
	 */
	@ColumnWidth(30)
	@ExcelProperty("产品名称")
	private String skuNameCn;

	/**
	 * 品类
	 */
	@ColumnWidth(15)
	@ExcelProperty("品类")
	private String categoryName;

	/**
	 * 销量
	 */
	@ColumnWidth(10)
	@ExcelProperty("销量")
	private Long quantity;

	/**
	 * 销售金额(₽)
	 */
	@ColumnWidth(15)
	@ExcelProperty("销售金额(₽)")
	private BigDecimal amount;

}
