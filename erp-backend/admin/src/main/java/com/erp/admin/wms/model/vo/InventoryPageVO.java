package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存明细分页VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存明细分页VO")
public class InventoryPageVO {

    @Schema(title = "库存ID")
    private Long id;

    @Schema(title = "仓库ID")
    private Long warehouseId;

	@Schema(title = "仓库展示信息")
	private WarehouseDisplayVO warehouseDisplay;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

    @Schema(title = "可用库存数量")
    private Integer availableQuantity;

    @Schema(title = "占用库存数量")
    private Integer reservedQuantity;

    @Schema(title = "在途库存数量")
    private Integer inTransitQuantity;

    @Schema(title = "残品库存数量")
    private Integer damagedQuantity;

    @Schema(title = "FBO最后同步时间")
    private LocalDateTime syncTime;

    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 获取仓内库存数量（计算字段）
     * 仓内库存 = 可用 + 占用（不含在途、不含残品）
     */
    @Schema(title = "仓内库存数量")
    public Integer getWarehouseQuantity() {
        int available = this.availableQuantity != null ? this.availableQuantity : 0;
        int reserved = this.reservedQuantity != null ? this.reservedQuantity : 0;
        return available + reserved;
    }

}
