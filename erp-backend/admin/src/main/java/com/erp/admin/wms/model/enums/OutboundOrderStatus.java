package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出库单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OutboundOrderStatus {

    WAITING_TRANSFER("待库位调整"),
    DRAFT("草稿"),
    CONFIRMED("待下架"),
    CANCELLED("已取消"),
    // 平台出库作业扩展状态（货主侧仅产生 DRAFT/CONFIRMED/CANCELLED；以下由平台作业流转）
    PICKING("拣货中"),
    PICKED("拣货完成"),
    BACKORDER("缺货挂起"),
    PACKED("已打包"),
    SHIPPED("已完成"),
    COMPLETED("已完成（历史兼容）");

    private final String description;

}
