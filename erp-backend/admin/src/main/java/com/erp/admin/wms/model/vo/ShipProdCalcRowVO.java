package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 发货生产测算汇总行（一行一个 SKU）。
 *
 * @author erp
 */
@Data
@Builder
public class ShipProdCalcRowVO {

    private String skuCode;

    private SkuBriefVO skuBrief;

    /** A+B 现货（海外仓+FBO） */
    private int onHand;

    /** C 在途（物流单已发未收） */
    private int inTransit;

    /** D+E 在制（采购单未发货，方案甲当 E） */
    private int producing;

    /** Xt 常态日均 */
    private double xt;

    /** Yt 巅峰日均 */
    private double yt;

    /** Zt 估算日均 */
    private double zt;

    /** 现货支撑天数 S（∞ 用 null 表示） */
    private Double shipSupportDays;

    /** 发货全链路支撑天数 S_total */
    private Double totalSupportDays;

    /** 生产全链路支撑天数 S_prod_total */
    private Double prodSupportDays;

    private boolean needShip;
    private int shipPlanQty;
    private String shipPath;

    private boolean needProduce;
    private int prodPlanQty;
    private String prodPath;

    /** 断档预警：现货撑不到最早到货 */
    private boolean shortage;

    /** 待观察：估算日销为 0 */
    private boolean noSales;

    private List<String> warnings;
}
