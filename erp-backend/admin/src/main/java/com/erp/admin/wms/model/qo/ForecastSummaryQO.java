package com.erp.admin.wms.model.qo;

import com.erp.admin.wms.model.enums.ForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "库存预测汇总查询条件")
public class ForecastSummaryQO {

    @Schema(title = "区域ID", description = "不传则查询所有区域")
    private Long regionId;

    @Schema(title = "SKU关键字")
    private String skuKeyword;

    @Schema(title = "库存状态")
    private ForecastStatus status;

    @Schema(title = "预测周期（天）", defaultValue = "30")
    private Integer days = 30;
}
