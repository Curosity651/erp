package com.erp.admin.wms.calc;

import lombok.Builder;
import lombok.Data;

/**
 * 发货测算结果（移植自 model_core.ShippingResult）。
 *
 * @author erp
 */
@Data
@Builder
public class ShippingResult {

    /** 现货可售支撑天数 S */
    private double s;

    /** ΔtC 最早在途到货距今天数（无在途为 null） */
    private Double dtC;

    /** 全链路总支撑天数 */
    private double sTotal;

    /** 是否需要发货（S_total < 35 天） */
    private boolean needShip;

    /** F = Z(t+2) × 45（仅需要发货时给出） */
    private int planQty;

    /** 计算路径说明（无在途 / 接力 / 全渠道） */
    private String path;
}
