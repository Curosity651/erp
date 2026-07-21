package com.erp.admin.wms.model.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销售出库单导出视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "销售出库单导出视图对象")
public class SalesOutboundExportVO {

    @ExcelIgnore
    @Schema(title = "主键ID")
    private Long id;

    @ExcelIgnore
    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @ExcelProperty("出库单号")
    @ColumnWidth(20)
    @Schema(title = "出库单号")
    private String outboundNo;

    @ExcelProperty("平台")
    @ColumnWidth(14)
    @Schema(title = "平台")
    private String platform;

    @ExcelProperty("出库仓库")
    @ColumnWidth(18)
    @Schema(title = "出库仓库名称")
    private String warehouseName;

    @ExcelProperty("出库日期")
    @ColumnWidth(14)
    @Schema(title = "出库日期")
    private String outboundDate;

    @ExcelProperty("订单数量")
    @ColumnWidth(10)
    @Schema(title = "订单数量")
    private Integer orderCount;

    @ExcelProperty("SKU数量")
    @ColumnWidth(10)
    @Schema(title = "SKU数量")
    private Integer skuCount;

    @ExcelProperty("出库总数量")
    @ColumnWidth(12)
    @Schema(title = "出库总数量")
    private Integer totalQuantity;

    @ExcelProperty("单据状态")
    @ColumnWidth(12)
    @Schema(title = "单据状态")
    private String orderStatus;

    @ExcelProperty("创建人")
    @ColumnWidth(14)
    @Schema(title = "创建人名称")
    private String createByName;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    @Schema(title = "创建时间")
    private String createTime;

    @ExcelProperty("备注")
    @ColumnWidth(24)
    @Schema(title = "备注")
    private String remark;

}
