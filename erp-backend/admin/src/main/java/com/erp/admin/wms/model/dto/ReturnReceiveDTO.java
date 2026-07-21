package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 退货收货入参。
 *
 * @author erp
 */
@Data
@Schema(title = "退货收货入参")
public class ReturnReceiveDTO {

    @NotNull(message = "退货单ID不能为空")
    @Schema(title = "退货单ID")
    private Long returnOrderId;

    @NotNull(message = "收货明细不能为空")
    @Schema(title = "收货明细")
    private List<ReceiveLine> items;

    @Data
    @Schema(title = "退货收货明细行")
    public static class ReceiveLine {

        @Schema(title = "SKU编码")
        private String skuCode;

        @Schema(title = "实收数")
        private Integer receivedQty;

    }

}
