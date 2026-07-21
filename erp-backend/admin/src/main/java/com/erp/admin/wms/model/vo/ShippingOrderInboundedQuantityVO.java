package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 物流单已入库数量汇总 VO
 *
 * @author erp
 */
@Data
public class ShippingOrderInboundedQuantityVO {

    /**
     * 物流单ID
     */
    private Long shippingOrderId;

    /**
     * 已入库总数量
     */
    private Integer inboundedQuantity;

}
