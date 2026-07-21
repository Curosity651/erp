package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 单 SKU 发货生产测算详情（数据链，含公式代入所需的全部中间量）。
 *
 * @author erp
 */
@Data
@Builder
public class ShipProdCalcDetailVO {

    private String skuCode;
    private SkuBriefVO skuBrief;

    private LocalDate baseDate;

    // —— 库存构成 ——
    private int overseas;      // A
    private int fbo;           // B
    private int inTransit;     // C 合计
    private int factoryDone;   // D
    private int producing;     // E 合计

    // —— 销量指标 ——
    private double xt;
    private double yt;
    private double zt;
    private double zt1;
    private double zt2;
    private double zt3;
    private double avg7;
    private double avg15;
    private double avg30;
    private double kBase;
    private int peakSamples;
    private int historyDays;

    // —— 发货链 ——
    private Double shipSupportDays;   // S 现货
    private Double shipDtC;           // ΔtC 天
    private Double totalSupportDays;  // S_total
    private boolean needShip;
    private int shipPlanQty;          // F
    private String shipPath;

    // —— 生产链 ——
    private Double prodSupportDays;   // S_prod 基础
    private Double prodDtE;           // ΔtE 天
    private Double prodTotalSupportDays; // S_prod_total
    private boolean needProduce;
    private int prodPlanQty;          // Q
    private String prodPath;

    // —— 时间轴（展示用日期）——
    private LocalDate earliestArrivalDate;    // 今天 + ΔtC
    private LocalDate earliestCompletionDate; // 今天 + ΔtE
    private LocalDate stockoutDate;           // 今天 + S_total（发货口径断货日）
    private LocalDate shipRedLineDate;        // 今天 + 35
    private LocalDate prodRedLineDate;        // 今天 + 80

    // —— 批次明细 ——
    private List<BatchVO> transitBatches;   // C
    private List<BatchVO> producingBatches; // E

    private List<String> warnings;

    @Data
    @Builder
    public static class BatchVO {
        private int qty;
        private LocalDate eta;
        private String label;
    }
}
