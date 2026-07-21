package com.erp.admin.order.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拣货单中的一行（一个订单的一个 SKU）。
 * <p>
 * 直接作为 FastExcel 的写出模型，列顺序由 {@code index} 决定。
 *
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonPickListRowVO {

    @ExcelProperty(value = "序号", index = 0)
    private Integer seq;

    @ExcelProperty(value = "平台订单号", index = 1)
    private String platformOrderId;

    @ExcelProperty(value = "发货单号", index = 2)
    private String postingNumber;

    @ExcelProperty(value = "SKU编码", index = 3)
    private String skuCode;

    @ExcelProperty(value = "商品名称", index = 4)
    private String skuName;

    @ExcelProperty(value = "数量", index = 5)
    private Integer quantity;

    /** SKU 映射缺失时在此列标注，便于货主回头补映射 */
    @ExcelProperty(value = "备注", index = 6)
    private String remark;
}
