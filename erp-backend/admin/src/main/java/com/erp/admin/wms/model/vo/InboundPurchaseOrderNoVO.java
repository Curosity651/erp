package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 入库单关联采购单号 VO
 *
 * @author erp
 */
@Data
public class InboundPurchaseOrderNoVO {

    /**
     * 入库单ID
     */
    private Long inboundOrderId;

    /**
     * 采购单号
     */
    private String purchaseOrderNo;

}
