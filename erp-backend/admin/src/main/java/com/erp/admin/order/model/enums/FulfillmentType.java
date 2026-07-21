package com.erp.admin.order.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 履约类型枚举
 * <p>
 * 定义订单的履约方式：
 * <ul>
 *   <li>FBS - 商家自发货（Fulfillment by Seller）</li>
 *   <li>FBO - 平台仓配（Fulfillment by Operator）</li>
 * </ul>
 *
 * @author erp
 */
@Getter
@RequiredArgsConstructor
public enum FulfillmentType {

    FBS("fbs", "商家自发货"),
    FBO("fbo", "平台仓配");

    private final String value;
    private final String description;

    /**
     * 判断值是否匹配（忽略大小写）
     * <p>
     * 用于替代魔法字符串比较，提高代码可读性。
     * <pre>
     * // 推荐用法
     * if (FulfillmentType.FBS.matches(order.getFulfillmentType())) { ... }
     *
     * // 不推荐
     * if ("fbs".equalsIgnoreCase(order.getFulfillmentType())) { ... }
     * </pre>
     *
     * @param value 待匹配的值
     * @return 是否匹配
     */
    public boolean matches(String value) {
        return this.value.equalsIgnoreCase(value);
    }
}
