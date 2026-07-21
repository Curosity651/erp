package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 调整明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "调整明细视图对象")
public class AdjustmentItemVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "目标批次ID")
	private Long physicalInventoryId;

	@Schema(title = "库位编码")
	private String locationCode;

	@Schema(title = "品质 GOOD/DAMAGED")
	private String quality;

	@Schema(title = "调整数量（始终为正数）")
	private Integer quantity;

	@Schema(title = "备注")
	private String remark;

}
