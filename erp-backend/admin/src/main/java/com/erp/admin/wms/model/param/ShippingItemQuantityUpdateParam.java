package com.erp.admin.wms.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物流单明细数量更新参数
 *
 * @author erp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "物流单明细数量更新参数")
public class ShippingItemQuantityUpdateParam {

    @Schema(title = "物流单明细ID")
    private Long shippingOrderItemId;

    @Schema(title = "预期数量（占用数量）")
    private Integer expectedQuantity;

    @Schema(title = "实际数量（到货数量）")
    private Integer actualQuantity;

}
