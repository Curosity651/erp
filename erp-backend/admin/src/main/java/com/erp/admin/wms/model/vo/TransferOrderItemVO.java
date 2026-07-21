package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 调拨单明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "调拨单明细视图对象")
public class TransferOrderItemVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "源批次ID(A仓)")
	private Long sourcePhysicalInventoryId;

	@Schema(title = "目标库位编码(B仓)")
	private String targetLocationCode;

	@Schema(title = "调拨数量")
	private Integer quantity;

	@Schema(title = "实际入库数量")
	private Integer receivedQuantity;

	@Schema(title = "备注")
	private String remark;

}
