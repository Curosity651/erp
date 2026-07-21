package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 过账单明细查询条件
 *
 * @author erp
 */
@Data
@Schema(title = "过账单明细查询条件")
public class StockPostingItemQO {

    @NotNull(message = "过账单ID不能为空")
    @Schema(title = "过账单ID")
    private Long postingId;

    @Schema(title = "SKU编码")
    private String skuCode;

    /** 货主作用域（服务端按身份注入，忽略前端传入）：null=平台看全部；非空=IN 过滤。 */
    @Schema(hidden = true)
    private java.util.List<Long> erpTenantIds;

}
