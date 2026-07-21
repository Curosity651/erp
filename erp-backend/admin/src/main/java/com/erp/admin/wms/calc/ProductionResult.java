package com.erp.admin.wms.calc;

import lombok.Builder;
import lombok.Data;

/**
 * 生产测算结果（移植自 model_core.ProductionResult）。
 *
 * @author erp
 */
@Data
@Builder
public class ProductionResult {

    /** 全渠道基础支撑天数 */
    private double sProd;

    /** ΔtE 最早在制完工距今天数（无在制为 null） */
    private Double dtE;

    /** 全链路总支撑天数 */
    private double sProdTotal;

    /** 是否需要订货（S_prod_total < 80 天） */
    private boolean needProduce;

    /** Q = Z(t+3) × 45（仅需要订货时给出） */
    private int planQty;

    /** 计算路径说明（无在制 / 接力 / 全渠道） */
    private String path;
}
