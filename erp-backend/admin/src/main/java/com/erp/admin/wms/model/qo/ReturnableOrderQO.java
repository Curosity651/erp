package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可退货订单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "可退货订单查询对象")
public class ReturnableOrderQO {

    @Schema(title = "关键字（订单号/SKU编码）")
    private String keyword;

    @Schema(title = "平台")
    private String platform;

}
