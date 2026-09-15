package com.erp.admin.order.model.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import com.erp.admin.product.excel.ImageData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.net.URL;
import java.time.LocalDateTime;

/**
 * Wildberries 订单导出对象
 *
 * @author system
 */
@ContentRowHeight(60)
@Data
@Schema(title = "Wildberries 订单导出对象")
public class WbOrderExportVO implements ImageData {

	@ColumnWidth(12)
	@ExcelProperty("平台")
	private String platform;

	@ColumnWidth(15)
	@ExcelProperty("店铺")
	private String erpShopName;

	@ColumnWidth(15)
	@ExcelProperty("SKU")
	private String skuCode;

	@ColumnWidth(10)
	@ExcelProperty("SkuNO")
	private Integer skuNo;

	@ExcelIgnore
	private URL skuImage;

	@ColumnWidth(15)
	@ExcelProperty(value = "图片")
	private byte[] imageBytes;

	@Override
	public URL getImageUrl() {
		return skuImage;
	}

	@Override
	public void setImageBytes(byte[] bytes) {
		this.imageBytes = bytes;
	}

	@ColumnWidth(15)
	@ExcelProperty("订单号")
	private String platformOrderId;

	@ColumnWidth(15)
	@ExcelProperty("订单总价格（RMB）")
	private String totalAmountCny;

	@ColumnWidth(12)
	@ExcelProperty("平台履约状态")
	private String platformStatusLabel;

	@ColumnWidth(20)
	@ExcelProperty("配送仓库名称")
	private String destinationWarehouseName;

	@ColumnWidth(30)
	@ExcelProperty("配送仓库地址")
	private String destinationWarehouseAddress;

	@ColumnWidth(12)
	@ExcelProperty("锁定状态")
	private String lockStatus;

	@ColumnWidth(20)
	@ExcelProperty("订单创建时间（莫斯科时区）")
	private LocalDateTime platformCreatedAtMoscow;
}
