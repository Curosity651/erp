package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存汇总统计VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存汇总统计")
public class InventorySummaryVO {

    @Schema(title = "SKU总数")
    private Integer totalSkuCount;

    @Schema(title = "可用库存数量")
    private Integer availableQuantity;

    @Schema(title = "占用库存数量")
    private Integer reservedQuantity;

    @Schema(title = "在途库存数量")
    private Integer inTransitQuantity;

    @Schema(title = "残品库存数量")
    private Integer damagedQuantity;

    @Schema(title = "仓库数量")
    private Integer warehouseCount;

    @Schema(title = "总体积(m³)")
    private BigDecimal totalVolume;

    @Schema(title = "缺失尺寸的SKU数量")
    private Integer volumeMissingSkuCount;

    /**
     * 获取仓内库存数量（计算字段）
     * 仓内库存 = 可用 + 占用
     */
    @Schema(title = "仓内库存数量")
    public Integer getWarehouseQuantity() {
        int available = this.availableQuantity != null ? this.availableQuantity : 0;
        int reserved = this.reservedQuantity != null ? this.reservedQuantity : 0;
        return available + reserved;
    }

}
