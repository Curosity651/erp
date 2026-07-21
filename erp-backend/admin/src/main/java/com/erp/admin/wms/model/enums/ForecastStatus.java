package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存预测状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum ForecastStatus {
    /** 充足：可售天数 > 阈值×2 */
    SUFFICIENT("充足"),
    /** 偏低：阈值 < 可售天数 ≤ 阈值×2 */
    LOW("偏低"),
    /** 告急：0 < 可售天数 ≤ 阈值，触发通知 */
    CRITICAL("告急"),
    /** 断货：可售天数 ≤ 0，触发通知 */
    STOCKOUT("断货"),
    /** 待观察：日均销量 = 0，无法预测 */
    NO_SALES("待观察");

    private final String label;

    /**
     * 根据可售天数和阈值计算状态
     * <p>
     * 注意：日均销量为0时应直接返回 NO_SALES，不应调用此方法
     *
     * @param sellableDays 可售天数（null 表示无销量数据）
     * @param thresholdDays 预警阈值天数
     * @return 库存状态
     */
    public static ForecastStatus fromSellableDays(Integer sellableDays, int thresholdDays) {
        if (sellableDays == null) {
            return NO_SALES;
        }
        if (sellableDays <= 0) {
            return STOCKOUT;
        }
        if (sellableDays <= thresholdDays) {
            return CRITICAL;
        }
        if (sellableDays <= thresholdDays * 2) {
            return LOW;
        }
        return SUFFICIENT;
    }

    /**
     * 基于库存量和动态安全库存判定状态
     * <p>
     * 用于逐日预测中，基于当日 closingStock 与 effectiveSafetyStock 的比较
     *
     * @param closingStock    期末库存
     * @param effectiveSafety 有效安全库存 = max(safetyStock, dailySales × thresholdDays)
     * @return 库存状态
     */
    public static ForecastStatus fromStock(int closingStock, int effectiveSafety) {
        if (closingStock <= 0) {
            return STOCKOUT;
        }
        if (closingStock <= effectiveSafety) {
            return CRITICAL;
        }
        if (closingStock <= effectiveSafety * 2) {
            return LOW;
        }
        return SUFFICIENT;
    }
}
