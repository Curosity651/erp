package com.erp.admin.wms.model.dto;

import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 库存过账明细DTO
 * 业务层构建完整的过账明细信息
 */
@Data
@Builder(toBuilder = true)
public class StockPostingItemDTO {

	/** 仓库ID（仓库级操作时填实际值，区域级操作时为 0） */
	private Long warehouseId;

	/** 区域ID（区域级操作时填实际值，仓库级操作时为 0） */
	private Long regionId;

	/** 货主维（这批货的主人），用于区域库存按货主隔离 + 货主级过账溯源；由上游 facade 从单据货主填入 */
	private Long erpTenantId;

	/** 服务商维（货架承租方），用于仓库/服务商级过账溯源（可空） */
	private Long wmsTenantId;

	/** SKU编码 */
	@NotBlank(message = "SKU编码不能为空")
	private String skuCode;

	/** 库存桶 */
	@NotNull(message = "库存桶不能为空")
	private StockBucket bucket;

	/** 方向 */
	@NotNull(message = "方向不能为空")
	private StockDirection direction;

	/** 数量（正数） */
	@NotNull(message = "数量不能为空")
	private Integer quantity;

	/** 备注 */
	private String remark;

}
