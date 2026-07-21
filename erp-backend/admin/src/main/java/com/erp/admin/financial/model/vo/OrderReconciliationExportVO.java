package com.erp.admin.financial.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import com.erp.admin.product.excel.SafeUrlImageConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;

/**
 * 订单财务对账导出 VO
 * <p>
 * 用于 Excel 导出，将 SkuBriefVO 的字段打平展示
 *
 * @author system
 */
@Data
@ContentRowHeight(60)
@Schema(title = "订单财务对账导出VO")
public class OrderReconciliationExportVO {

    // ==================== 店铺与订单基础信息 ====================

    @ColumnWidth(15)
    @ExcelProperty("店铺")
    @Schema(title = "店铺名称")
    private String shopName;

    @ColumnWidth(18)
    @ExcelProperty("平台订单号")
    @Schema(title = "平台订单号")
    private String platformOrderId;

    @ColumnWidth(18)
    @ExcelProperty("订单RID")
    @Schema(title = "订单RID")
    private String rid;

    // ==================== SKU信息（打平展示） ====================

    @ColumnWidth(15)
    @ExcelProperty("商品编码")
    @Schema(title = "商品编码 (WB平台商家自录入)")
    private String article;

    @ColumnWidth(15)
    @ExcelProperty("SKU编码")
    @Schema(title = "ERP SKU 编码")
    private String skuCode;

    @ColumnWidth(15)
    @ExcelProperty(value = "SKU图片", converter = SafeUrlImageConverter.class)
    @Schema(title = "ERP SKU 主图")
    private URL skuImage;

    // ==================== 订单详情 ====================

    @ColumnWidth(8)
    @ExcelProperty("数量")
    @Schema(title = "商品数量")
    private Integer quantity;

    @ColumnWidth(10)
    @ExcelProperty("履约类型")
    @Schema(title = "履约类型", allowableValues = {"FBS", "FBO"})
    private String fulfillmentType;

    // ==================== 状态信息 ====================

    @ColumnWidth(12)
    @ExcelProperty("ERP状态")
    @Schema(title = "ERP状态")
    private String erpStatus;

    @ColumnWidth(15)
    @ExcelProperty("商家处理状态")
    @Schema(title = "商家处理状态（platformSubstatus/supplierStatus）")
    private String platformSubstatus;

    @ColumnWidth(12)
    @ExcelProperty("平台履约状态")
    @Schema(title = "平台履约状态（platformStatus/wbStatus）")
    private String platformStatus;

    @ColumnWidth(12)
    @ExcelProperty("对账状态")
    @Schema(title = "对账状态：MATCHED-已对账, PENDING-待履约, IN_TRANSIT-运输中, CANCELED-已取消, ANOMALY-异常")
    private String reconciliationStatus;

    // ==================== 财务汇总信息 ====================

    @ColumnWidth(12)
    @ExcelProperty("财务记录数")
    @Schema(title = "财务记录数量")
    private Integer financialRecordCount;

    @ColumnWidth(15)
    @ExcelProperty("销售金额")
    @Schema(title = "正向销售金额（supplier_oper_name 为销售的 ppvz_for_pay 合计）")
    private BigDecimal saleAmount;

    @ColumnWidth(15)
    @ExcelProperty("实收金额")
    @Schema(title = "实收金额（所有财务记录 ppvz_for_pay 合计）")
    private BigDecimal actualIncome;

    @ColumnWidth(10)
    @ExcelProperty("财务币种")
    @Schema(title = "财务记录币种", example = "руб")
    private String financialCurrencyName;

    // ==================== 金额与时间 ====================

    @ColumnWidth(22)
    @ExcelProperty("订单时间(莫斯科)")
    @Schema(title = "订单时间（莫斯科时区）")
    private LocalDateTime orderTimeMoscow;

    @ColumnWidth(15)
    @ExcelProperty("订单金额(卢布)")
    @Schema(title = "订单总金额（卢布）")
    private BigDecimal totalAmountRub;

    @ColumnWidth(15)
    @ExcelProperty("订单金额(CNY)")
    @Schema(title = "转换后金额（CNY）")
    private BigDecimal convertedAmount;

}
