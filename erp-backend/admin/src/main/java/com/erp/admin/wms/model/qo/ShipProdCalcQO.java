package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 发货生产测算查询条件。
 *
 * @author erp
 */
@Data
@Schema(title = "发货生产测算查询条件")
public class ShipProdCalcQO {

    @Schema(title = "SKU 关键字（编码/名称）")
    private String skuKeyword;

    @Schema(title = "决策筛选：SHIP 需发货 / PRODUCE 需订货 / SHORTAGE 断档 / NO_SALES 待观察；空=全部")
    private String decision;

    @Schema(title = "测算基准日（默认今天）")
    private LocalDate baseDate;
}
