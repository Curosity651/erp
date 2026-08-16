package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库位调整单明细视图对象。
 *
 * @author erp
 */
@Data
@Schema(title = "库位调整单明细视图对象")
public class LocationTransferItemVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "SKU编码")
	private String skuCode;

	private String warehouseSkuCode;

	@Schema(title = "SKU展示信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "源批次ID")
	private Long physicalInventoryId;

	@Schema(title = "源逻辑库位库存ID")
	private Long sourceInventoryId;

	@Schema(title = "源库位编码")
	private String sourceLocationCode;

	@Schema(title = "源库位分区名称")
	private String sourceZoneName;

	@Schema(title = "源品质 GOOD/DAMAGED")
	private String sourceQuality;

	@Schema(title = "调整方式：PARTIAL/WHOLE_PALLET")
	private String moveMode;

	@Schema(title = "源托盘ID")
	private Long sourcePalletId;

	@Schema(title = "源托盘号")
	private String sourcePalletNo;

	@Schema(title = "源托位ID")
	private Long sourceSlotId;

	@Schema(title = "源托位编码")
	private String sourceSlotCode;

	@Schema(title = "目标库位编码")
	private String targetLocationCode;

	@Schema(title = "目标逻辑库位ID")
	private Long targetLocationId;

	@Schema(title = "目标库位分区名称")
	private String targetZoneName;

	@Schema(title = "目标托位ID")
	private Long targetSlotId;

	@Schema(title = "目标托位编码")
	private String targetSlotCode;

	@Schema(title = "目标托盘ID")
	private Long targetPalletId;

	@Schema(title = "目标托盘号")
	private String targetPalletNo;

	@Schema(title = "移动数量")
	private Integer quantity;

	@Schema(title = "是否落库置为良品（1=是）")
	private Integer toGood;

	@Schema(title = "备注")
	private String remark;

}
