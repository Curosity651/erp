package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物流单分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单分页视图对象")
public class ShippingOrderPageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "物流单号")
	private String shippingNo;

	@Schema(title = "物流商ID")
	private Long providerId;

	@Schema(title = "物流商名称")
	private String providerName;

	@Schema(title = "发货日期")
	private LocalDate shippingDate;

	@Schema(title = "预计到货日期")
	private LocalDate estimatedArrivalDate;

	@Schema(title = "预计时效(天)")
	private Integer estimatedDays;

	@Schema(title = "物流方式: GRAY-灰关 / WHITE-白关")
	private String shippingMethod;

	@Schema(title = "物流线路: EAST-东线 / WEST-西线 / RAIL-铁路")
	private String shippingRoute;

	@Schema(title = "目标区域ID")
	private Long targetRegionId;

	@Schema(title = "目标区域信息")
	private RegionDisplayVO targetRegion;

	@Schema(title = "发货件数")
	private Integer packageCount;

	@Schema(title = "SKU种类数")
	private Integer skuCount;

	@Schema(title = "总发货数量")
	private Integer totalShippedQuantity;

	@Schema(title = "已入库数量")
	private Integer totalReceivedQuantity;

	@Schema(title = "Remaining quantity available for inbound order creation")
	private Integer pendingInboundQuantity;

	@Schema(title = "总重量(KG)")
	private BigDecimal totalWeight;

	@Schema(title = "物流总金额(USD)")
	private BigDecimal totalAmount;

	@Schema(title = "付款状态: 0-未付 / 1-已付")
	private Integer paymentStatus;

	@Schema(title = "物流单状态")
	private String shippingStatus;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
