package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可入库物流单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "可入库物流单查询对象")
public class AvailableShippingQO {

    @Schema(title = "物流单号")
    private String shippingNo;

    @Schema(title = "采购单号")
    private String purchaseOrderNo;

    @Schema(title = "物流商ID")
    private Long providerId;

}
