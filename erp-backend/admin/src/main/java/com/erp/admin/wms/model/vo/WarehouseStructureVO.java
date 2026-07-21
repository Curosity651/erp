package com.erp.admin.wms.model.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仓库结构参数视图（库位管理页用）。
 *
 * @author erp
 */
@Data
@Schema(title = "仓库结构参数")
public class WarehouseStructureVO {

	@Schema(title = "仓库ID")
	private Long id;

	@Schema(title = "仓库编码")
	private String warehouseCode;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "仓库类型")
	private String warehouseType;

	@Schema(title = "排数")
	private Integer rackRows;

	@Schema(title = "每排列数")
	private Integer rackColumns;

	@Schema(title = "排号前缀")
	private String rackNoPrefix;

	@Schema(title = "补零位宽")
	private Integer codePadWidth;

	@Schema(title = "默认库位类型")
	private String defaultLocationType;

	@Schema(title = "库位已生成 1是/0否")
	private Integer locationGenerated;

	private Integer palletLevels;
	private Integer maxSkuKindsPerPallet;
	private Integer allowCrossOwnerMix;
	private Integer defaultPalletLengthMm;
	private Integer defaultPalletWidthMm;
	private Integer defaultPalletHeightMm;
	private java.math.BigDecimal defaultPalletMaxWeightKg;
	private java.math.BigDecimal defaultPalletUtilization;

	@Schema(title = "结构是否锁定（有货占用或已分配服务商 → 禁改结构/重新生成）")
	private Boolean structureLocked;

	@Schema(title = "有货物占用")
	private Boolean occupied;

	@Schema(title = "有货占用的库位数")
	private Integer occupiedLocationCount;

	@Schema(title = "已分配给服务商（当前有效）")
	private Boolean assigned;

	@Schema(title = "已分配的货架排数（当前有效）")
	private Integer assignedRackCount;

	@Schema(title = "已分配的服务商名称（当前有效，去重）")
	private List<String> assignedOperatorNames;

}
