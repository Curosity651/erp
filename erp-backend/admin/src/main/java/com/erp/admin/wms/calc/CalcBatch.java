package com.erp.admin.wms.calc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 一个在途柜次 / 一笔在制生产：数量 + 预计可用日期（移植自 model_core.Batch）。
 *
 * @author erp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalcBatch {

    /** 数量 */
    private int qty;

    /** 预计可用日期；null = 日期未知（由调用方先用默认周期补齐） */
    private LocalDate eta;

    /** 柜号 / 单号，仅展示用 */
    private String label;

    public CalcBatch(int qty, LocalDate eta) {
        this(qty, eta, "");
    }
}
