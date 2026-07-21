package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 仓库库存汇总VO
 *
 * @author erp
 */
@Data
@Schema(title = "仓库库存汇总")
public class WarehouseSummaryVO {

    @Schema(title = "仓库ID")
    private Long warehouseId;

	@Schema(title = "仓库展示信息")
	private WarehouseDisplayVO warehouseDisplay;

    @Schema(title = "SKU数量")
    private Integer skuCount;

    @Schema(title = "可用库存数量")
    private Integer availableQuantity;

    @Schema(title = "占用库存数量")
    private Integer reservedQuantity;

    @Schema(title = "在途库存数量")
    private Integer inTransitQuantity;

    @Schema(title = "残品库存数量")
    private Integer damagedQuantity;

    @Schema(title = "总体积(m³)")
    private BigDecimal totalVolume;

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
