package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 采购入库单统计视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单统计视图对象")
public class PurchaseInboundStatsVO {

	@Schema(title = "入库单ID")
	private Long inboundOrderId;

	@Schema(title = "商品摘要")
	private String itemSummary;

	@Schema(title = "应到总数量")
	private Integer totalExpectedQty;

	@Schema(title = "实到总数量")
	private Integer totalActualQty;

	@Schema(title = "差异数量")
	private Integer totalShortQty;

	@Schema(title = "SKU种类数")
	private Integer skuCount;

	@Schema(title = "关联采购单号列表")
	private List<String> purchaseOrderNos;

}
