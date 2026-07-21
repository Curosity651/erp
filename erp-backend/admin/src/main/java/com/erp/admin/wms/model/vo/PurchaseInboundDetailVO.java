package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 采购入库单详情视图对象
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "采购入库单详情视图对象")
public class PurchaseInboundDetailVO extends PurchaseInboundPageVO {

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人姓名")
    private String createByName;

    @Schema(title = "入库明细列表")
    private List<PurchaseInboundItemVO> items;

    @Schema(title = "关联物流单信息（用于编辑页回填）")
    private AvailableShippingVO shippingOrder;

}
