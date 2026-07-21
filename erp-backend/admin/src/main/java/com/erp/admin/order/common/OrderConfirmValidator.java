package com.erp.admin.order.common;

import com.erp.admin.order.model.entity.ErpOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单确认验证器
 * <p>
 * 职责：
 * - 检查锁定状态
 * - 过滤平台和状态
 */
@Component
@Slf4j
public class OrderConfirmValidator {

    /**
     * 验证订单是否可确认
     *
     * @param orders          订单列表
     * @param platformCode    平台代码
     * @param allowedStatuses 允许的 ERP 状态
     * @return 符合条件的订单
     * @throws IllegalArgumentException 存在锁定订单时抛出
     */
    public List<ErpOrder> validate(List<ErpOrder> orders,
                                   String platformCode,
                                   List<String> allowedStatuses) {
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        // 检查锁定
        boolean hasLocked = orders.stream()
                .anyMatch(o -> o.getLocked() != null && o.getLocked() == 1);
        if (hasLocked) {
            throw new IllegalArgumentException("存在锁定订单，不允许确认");
        }

        // 过滤平台和状态
        return orders.stream()
                .filter(o -> {
                    if (!platformCode.equalsIgnoreCase(o.getPlatform())) {
                        log.warn("[{}][CONFIRM] 跳过非{}平台订单 orderId={} platform={}",
                                platformCode, platformCode, o.getId(), o.getPlatform());
                        return false;
                    }
                    if (!allowedStatuses.contains(o.getErpStatus())) {
                        log.warn("[{}][CONFIRM] 跳过不符合状态的订单 orderId={} status={}",
                                platformCode, o.getId(), o.getErpStatus());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
