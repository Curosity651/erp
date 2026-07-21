package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单被拒绝执行某操作时的原因说明（准备发运 / 拣货单共用）。
 *
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "订单不可执行原因")
public class OzonOrderRejectVO {

    @Schema(title = "erp_order.id")
    private Long id;

    private String platformOrderId;

    @Schema(title = "不可执行的原因，直接展示给货主")
    private String reason;
}
