package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 签出结果（回传跟踪号、渠道、物流费、计费流水ID）。
 *
 * @author erp
 */
@Data
@Schema(title = "签出结果")
public class ShipResultVO {

    private String trackingNo;

    private String channelName;

    private BigDecimal shippingFee;

    @Schema(title = "生成的计费流水ID(无计费则空)")
    private Long billingRecordId;

    @Schema(title = "本次仓储操作费(CNY)")
    private BigDecimal warehouseOperationFee;

}
