package com.erp.admin.wms.model.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 退货入库单导出视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "退货入库单导出视图对象")
public class ReturnInboundExportVO {

    @ExcelIgnore
    @Schema(title = "主键ID")
    private Long id;

    @ExcelProperty("退货单号")
    @ColumnWidth(20)
    @Schema(title = "退货单号")
    private String returnNo;

    @ExcelProperty("平台订单号")
    @ColumnWidth(20)
    @Schema(title = "平台订单号")
    private String platformOrderId;

    @ExcelProperty("入库仓库")
    @ColumnWidth(18)
    @Schema(title = "入库仓库名称")
    private String warehouseName;

    @ExcelProperty("退货日期")
    @ColumnWidth(14)
    @Schema(title = "退货日期")
    private String returnDate;

    @ExcelProperty("退货原因")
    @ColumnWidth(14)
    @Schema(title = "退货原因")
    private String returnReason;

    @ExcelProperty("退货总数量")
    @ColumnWidth(12)
    @Schema(title = "退货总数量")
    private Integer totalQuantity;

    @ExcelProperty("合格入库数量")
    @ColumnWidth(14)
    @Schema(title = "合格入库总数量")
    private Integer qualifiedQuantity;

    @ExcelProperty("不合格数量")
    @ColumnWidth(12)
    @Schema(title = "不合格总数量")
    private Integer unqualifiedQuantity;

    @ExcelProperty("备注")
    @ColumnWidth(24)
    @Schema(title = "备注")
    private String remark;

    @ExcelProperty("创建人")
    @ColumnWidth(14)
    @Schema(title = "创建人名称")
    private String createByName;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    @Schema(title = "创建时间")
    private String createTime;

}
