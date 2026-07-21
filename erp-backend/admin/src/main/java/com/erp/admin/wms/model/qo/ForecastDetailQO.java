package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(title = "库存预测详情查询条件")
public class ForecastDetailQO {

    @NotNull(message = "区域ID不能为空")
    @Schema(title = "区域ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long regionId;

    @NotBlank(message = "SKU编码不能为空")
    @Schema(title = "SKU编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuCode;

    @Schema(title = "预测周期（天）", defaultValue = "30")
    private Integer days = 30;

    @Schema(title = "日均销量覆盖值", description = "临时覆盖，不持久化")
    private Integer dailySales;
}
