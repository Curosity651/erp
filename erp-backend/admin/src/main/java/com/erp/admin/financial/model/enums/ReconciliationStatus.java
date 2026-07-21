package com.erp.admin.financial.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单财务对账状态枚举
 * <p>
 * 基于订单状态 + 财务记录综合判断对账情况
 *
 * @author system
 */
@Getter
@AllArgsConstructor
public enum ReconciliationStatus {

    /**
     * 待履约
     * 订单尚未发货，不需要财务记录
     */
    PENDING("待履约", "default"),

    /**
     * 运输中
     * 订单已发货但未交付，尚无财务记录是正常的
     */
    IN_TRANSIT("运输中", "processing"),

    /**
     * 已取消
     * 订单发货前取消，无需财务记录
     */
    CANCELED("已取消", "warning"),

    /**
     * 已对账
     * 订单履约完成，财务记录正常匹配
     */
    MATCHED("已对账", "success"),

    /**
     * 异常
     * 订单应有财务记录但缺失或无销售记录，需排查
     */
    ANOMALY("异常", "error");

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 前端展示样式
     * success: 绿色, processing: 蓝色, error: 红色, warning: 橙色, default: 灰色
     */
    private final String style;

    /**
     * 判断是否为异常状态
     *
     * @return true 如果是异常状态
     */
    public boolean isAnomaly() {
        return this == ANOMALY;
    }

    /**
     * 根据枚举名称获取描述
     *
     * @param name 枚举名称
     * @return 描述，如果找不到返回 null
     */
    public static String getDescriptionByName(String name) {
        if (name == null) {
            return null;
        }
        for (ReconciliationStatus status : values()) {
            if (status.name().equals(name)) {
                return status.getDescription();
            }
        }
        return null;
    }

    /**
     * 根据枚举名称获取样式
     *
     * @param name 枚举名称
     * @return 样式，如果找不到返回 default
     */
    public static String getStyleByName(String name) {
        if (name == null) {
            return "default";
        }
        for (ReconciliationStatus status : values()) {
            if (status.name().equals(name)) {
                return status.getStyle();
            }
        }
        return "default";
    }

    /**
     * 根据名称获取枚举
     *
     * @param name 枚举名称
     * @return 枚举值，如果找不到返回 null
     */
    public static ReconciliationStatus fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ReconciliationStatus status : values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        return null;
    }
}
