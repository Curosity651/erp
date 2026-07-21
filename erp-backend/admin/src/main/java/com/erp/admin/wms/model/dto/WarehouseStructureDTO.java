package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 仓库结构参数更新请求（C1）。
 *
 * @author erp
 */
@Data
@Schema(title = "仓库结构参数更新")
public class WarehouseStructureDTO {

	@NotNull(message = "仓库ID不能为空")
	@Schema(title = "仓库ID")
	private Long id;

	@Schema(title = "排数")
	private Integer rackRows;

	@Schema(title = "每排列数")
	private Integer rackColumns;

	@Schema(title = "排号前缀(A→A1;空=纯数字)")
	private String rackNoPrefix;

	@Schema(title = "补零位宽(2→03)")
	private Integer codePadWidth;

	@Schema(title = "默认库位类型 BIG/SMALL")
	private String defaultLocationType;

	@Schema(title = "每个二维库位的托盘层数，当前固定为3")
	private Integer palletLevels;

	@Schema(title = "单托最多不同货物种类")
	private Integer maxSkuKindsPerPallet;

	@Schema(title = "是否允许跨货主混托")
	private Integer allowCrossOwnerMix;

	private Integer defaultPalletLengthMm;
	private Integer defaultPalletWidthMm;
	private Integer defaultPalletHeightMm;
	private java.math.BigDecimal defaultPalletMaxWeightKg;
	private java.math.BigDecimal defaultPalletUtilization;

}
