package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 物流单简要视图对象（用于关联展示）
 *
 * @author erp
 */
@Data
@Schema(title = "物流单简要视图对象")
public class ShippingOrderSimpleVO {

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

	@Schema(title = "物流方式: GRAY-灰关 / WHITE-白关")
	private String shippingMethod;

	@Schema(title = "物流线路: EAST-东线 / WEST-西线 / RAIL-铁路")
	private String shippingRoute;

	@Schema(title = "物流单状态")
	private String shippingStatus;

	@Schema(title = "发货件数")
	private Integer packageCount;

	@Schema(title = "物流总金额(USD)")
	private BigDecimal totalAmount;

	@Schema(title = "付款状态: 0-未付 / 1-已付")
	private Integer paymentStatus;

	@Schema(title = "本采购单发货数量")
	private Integer shippedQuantity;

	@Schema(title = "本采购单已到货数量")
	private Integer receivedQuantity;

}
