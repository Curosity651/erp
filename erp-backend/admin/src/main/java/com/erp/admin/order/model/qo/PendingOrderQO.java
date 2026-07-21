package com.erp.admin.order.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 待出库订单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "待出库订单查询对象")
public class PendingOrderQO {

    @NotBlank(message = "平台不能为空")
    @Schema(title = "平台", requiredMode = Schema.RequiredMode.REQUIRED)
    private String platform;

    @NotNull(message = "仓库ID不能为空")
    @Schema(title = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseId;

    @Schema(title = "订单号关键词（模糊匹配）")
    private String keyword;

    @Schema(title = "SKU编码列表（精准匹配）")
    private List<String> skuCodes;
}
