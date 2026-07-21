package com.erp.admin.order.service.ozon.converter;

import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Ozon 订单状态转换器
 * <p>
 * 将 Ozon 平台的订单状态（status + substatus）转换为 ERP 统一状态。
 * 映射表经用户审批（2026-07-02），基于 Ozon Seller API 官方状态全集：
 * <ul>
 *   <li>awaiting_registration / acceptance_in_progress / awaiting_approve / awaiting_packaging
 *       → READY_TO_SHIP（待发货；前三个为备货前过渡态，可见但确认时由预同步/平台侧拦截）</li>
 *   <li>awaiting_deliver / delivering / driver_pickup / sent_by_seller → SHIPPED（发货中）</li>
 *   <li>delivered → DELIVERED（已送达）</li>
 *   <li>cancelled → CANCELED（已取消）</li>
 *   <li>arbitration / client_arbitration / not_accepted → null（仲裁/拒收需人工介入，
 *       保持订单现有 ERP 状态不变 + 告警日志）</li>
 *   <li>未知 / 空 → null（保持现状 + 告警；严禁回退成 READY_TO_SHIP，
 *       否则已发货订单会被打回、触发重复预占与二次确认）</li>
 * </ul>
 * null 语义与 Yandex 转换器一致：调用方对已存在订单保持原状态，对新订单跳过落库。
 * substatus 仅存储展示，不参与映射（本业务运营判定用不到子状态）。
 *
 * @author system
 */
@Slf4j
public final class OzonOrderStatusConverter {

    private OzonOrderStatusConverter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 将 Ozon 订单状态转换为 ERP 统一状态
     *
     * @param status    Ozon 订单主状态
     * @param substatus Ozon 订单子状态（仅用于日志）
     * @return ERP 统一状态枚举；null 表示保持现状（已存在订单不改状态，新订单跳过）
     */
    public static ErpOrderStatusEnum toErpStatus(String status, String substatus) {
        if (status == null || status.isEmpty()) {
            log.warn("[OZON] 订单状态为空，保持 ERP 状态不变: substatus={}", substatus);
            return null;
        }

        String normalizedStatus = status.toLowerCase().trim();

        switch (normalizedStatus) {
            // 备货前过渡态 + 等待备货 → 待发货
            case "awaiting_registration":
            case "acceptance_in_progress":
            case "awaiting_approve":
            case "awaiting_packaging":
                return ErpOrderStatusEnum.READY_TO_SHIP;

            // 等待发运 + 在途各阶段 → 发货中（进销售出库池）
            case "awaiting_deliver":
            case "delivering":
            case "driver_pickup":
            case "sent_by_seller":
                return ErpOrderStatusEnum.SHIPPED;

            case "delivered":
                return ErpOrderStatusEnum.DELIVERED;

            case "cancelled":
                return ErpOrderStatusEnum.CANCELED;

            // 仲裁/分拣中心拒收：需人工去平台后台处理，ERP 状态保持不变
            case "arbitration":
            case "client_arbitration":
            case "not_accepted":
                log.warn("[OZON] 订单进入需人工介入状态，保持 ERP 状态不变: status={}, substatus={}",
                        status, substatus);
                return null;

            // 未知状态：保持现状 + 告警，提示补充映射
            default:
                log.warn("[OZON] 未映射的订单状态，保持 ERP 状态不变: status={}, substatus={}",
                        status, substatus);
                return null;
        }
    }

    /**
     * 将 Ozon 订单状态转换为 ERP 统一状态（仅使用主状态）
     *
     * @param status Ozon 订单主状态
     * @return ERP 统一状态枚举；null 表示保持现状
     */
    public static ErpOrderStatusEnum toErpStatus(String status) {
        return toErpStatus(status, null);
    }
}
