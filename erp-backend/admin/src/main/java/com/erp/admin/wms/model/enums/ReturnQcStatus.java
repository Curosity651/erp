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

    RETURN_PENDING("待退货收货"),
    QC_PENDING("待质检"),
    COMPLETED("退货入库完成"),
    CLOSED("已关闭");

    private final String description;

}
