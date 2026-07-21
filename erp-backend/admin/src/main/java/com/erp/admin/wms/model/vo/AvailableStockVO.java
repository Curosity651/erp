package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可调拨库存视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "可调拨库存视图对象")
public class AvailableStockVO {

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "可用库存数量")
	private Integer availableQuantity;

}
