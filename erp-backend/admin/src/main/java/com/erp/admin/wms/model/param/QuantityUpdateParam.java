package com.erp.admin.wms.model.param;

import lombok.Builder;
import lombok.Data;

/**
 * 数量更新参数
 *
 * @author erp
 */
@Data
@Builder
public class QuantityUpdateParam {

    /**
     * 采购单明细ID
     */
    private Long purchaseOrderItemId;

    /**
     * 数量
     */
    private Integer quantity;

}
