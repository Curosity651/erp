package com.erp.admin.order.service.label.model;

import com.erp.admin.order.model.entity.ErpOrder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单失败信息
 * <p>
 * 封装订单及其失败原因，用于批量打印时记录不可打印订单的失败信息。
 * <p>
 * 在订单分类阶段，策略通过 {@code checkPrintability()} 方法返回此对象表示订单不可打印，
 * 编排器会收集所有失败信息并统一标记到批次项中。
 *
 * @author system
 */
@Getter
@RequiredArgsConstructor
public class OrderFailureInfo {

    /**
     * 失败的订单
     */
    private final ErpOrder order;

    /**
     * 失败代码（对应 LabelConstants.ITEM_FAIL_* 常量）
     */
    private final String failureCode;

    /**
     * 错误信息（用户可读的失败原因）
     */
    private final String errorMessage;
}
