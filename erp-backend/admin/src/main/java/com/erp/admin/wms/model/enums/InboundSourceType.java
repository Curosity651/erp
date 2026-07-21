package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 入库单来源（{@code wms_purchase_inbound_order.source_type}）。
 * <p>
 * PURCHASE：经「采购 → 物流 → 入库」完整链路，关联物流单/采购单。 MANUAL：自定义入库，不经采购链（调拨、退货归仓、期初等）。
 * CUSTOM_RETURN：自定义退货，不挂靠平台订单的退货入库（平台批量退货混包、无单退件、样品收回、发错召回等）。
 * </p>
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum InboundSourceType {

    PURCHASE("采购入库"),
    MANUAL("自定义入库"),
    CUSTOM_RETURN("自定义退货");

    private final String description;

}
