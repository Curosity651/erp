package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 采购入库单简要信息
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单简要信息")
public class PurchaseInboundSimpleVO {

	@Schema(title = "入库单ID")
	private Long id;

	@Schema(title = "入库单号")
	private String inboundNo;

	@Schema(title = "入库仓库ID")
	private Long warehouseId;

	@Schema(title = "入库仓库名称")
	private String warehouseName;

	@Schema(title = "入库日期")
	private LocalDate inboundDate;

	@Schema(title = "单据状态")
	private String orderStatus;

	@Schema(title = "SKU数量")
	private Integer skuCount;

	@Schema(title = "入库总数量")
	private Integer totalQuantity;

}
