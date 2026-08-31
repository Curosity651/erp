package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存明细详情VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存明细详情VO")
public class InventoryDetailVO {

	@Schema(title = "库存ID")
	private Long id;

	@Schema(title = "仓库ID")
	private Long warehouseId;

	@Schema(title = "仓库展示信息")
	private WarehouseDisplayVO warehouseDisplay;

	@Schema(title = "SKU编码")
	private String skuCode;

	private Integer physicalQuantity;

	private Integer damagedQuantity;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "可用库存数量")
	private Integer availableQuantity;

	@Schema(title = "占用库存数量")
	private Integer reservedQuantity;

	@Schema(title = "在途库存数量")
	private Integer inTransitQuantity;

	@Schema(title = "FBO最后同步时间")
	private LocalDateTime syncTime;

	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	/**
	 * 获取总库存数量（计算字段）
	 */
	@Schema(title = "总库存数量")
	public Integer getTotalQuantity() {
		int available = this.availableQuantity != null ? this.availableQuantity : 0;
		int reserved = this.reservedQuantity != null ? this.reservedQuantity : 0;
		int inTransit = this.inTransitQuantity != null ? this.inTransitQuantity : 0;
		return available + reserved + inTransit;
	}


	@Schema(title = "最近流水列表")
    private List<StockFlowDetailVO> recentFlows;

}
