package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退货质检状态机（业务需求 1.4）。
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum ReturnQcStatus {

    PENDING_OWNER("待货主处置"),
    PENDING_OPERATION("待仓库处理"),
    COMPLETED("退货处理完成"),
    CLOSED("已关闭"),
    /** Historical states retained only for reading pre-migration records. */
    RETURN_PENDING("历史待收货"),
    QC_PENDING("历史待质检");

    private final String description;

}
