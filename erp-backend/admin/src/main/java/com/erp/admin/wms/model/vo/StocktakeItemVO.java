package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 盘点明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点明细视图对象")
public class StocktakeItemVO {

	@Schema(title = "主键ID")
	private Long id;

	private Long locationTaskId;

	private Long physicalInventoryId;

	@Schema(title = "货主ID（货物归属；同一skuCode在不同货主下可重名，须以货主+skuCode区分）")
	private Long erpTenantId;

	private Long wmsTenantId;

	@Schema(title = "货主名称")
	private String ownerName;

	@Schema(title = "SKU编码")
	private String skuCode;

	private Long zoneId;

	private String locationCode;

	private String quality;

	private Integer allocatable;

	private java.time.LocalDate inboundDate;

	private Integer pickOrder;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "系统数量")
	private Integer systemQuantity;

	private Integer reservedQuantity;

	@Schema(title = "实盘数量")
	private Integer actualQuantity;

	@Schema(title = "差异数量（实盘数量 - 系统数量）")
	private Integer diffQuantity;

	@Schema(title = "盘点状态: PENDING-未盘 / COUNTED-已盘")
	private String stocktakeStatus;

	private String reviewStatus;

	@Schema(title = "SKU来源: EXISTING-库内 / ADDED-追加")
	private String sourceType;

	@Schema(title = "SKU来源描述")
	private String sourceTypeDesc;

	@Schema(title = "备注")
	private String remark;

}
