package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU库存汇总VO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU库存汇总")
public class SkuSummaryVO {

    @Schema(title = "SKU编码")
    private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

    @Schema(title = "分布仓库数")
    private Integer warehouseCount;

    @Schema(title = "可用库存数量")
    private Integer availableQuantity;

    @Schema(title = "占用库存数量")
    private Integer reservedQuantity;

    @Schema(title = "在途库存数量")
    private Integer inTransitQuantity;

    @Schema(title = "残品库存数量")
    private Integer damagedQuantity;

    @Schema(title = "FBO仓内库存")
    private Integer fboWarehouseQuantity;

    @Schema(title = "自有仓仓内库存")
    private Integer ownWarehouseQuantity;

    @Schema(title = "单件体积(m³)")
    private BigDecimal unitVolume;

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

    @Schema(title = "货主持有总量（海外仓仓内 + FBO）")
    public Integer getTotalHeldQuantity() {
        int own = getWarehouseQuantity();
        int fbo = this.fboWarehouseQuantity != null ? this.fboWarehouseQuantity : 0;
        return own + fbo;
    }

}
