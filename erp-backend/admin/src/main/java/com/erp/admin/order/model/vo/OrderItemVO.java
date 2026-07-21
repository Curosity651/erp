package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单商品明细VO
 *
 * @author system
 */
@Data
@Schema(title = "订单商品明细VO")
public class OrderItemVO {

	@Schema(title = "平台商品ID")
	private String platformItemId;

	@Schema(title = "内部SKU编码")
	private String skuCode;

	@Schema(title = "SKU名称（展示用，从sku表填充）")
	private String skuName;

	@Schema(title = "SKU主图（展示用，从sku表填充）")
	private String mainImage;

	@Schema(title = "数量")
	private Integer quantity;

	@Schema(title = "单价")
	private BigDecimal itemPrice;

	@Schema(title = "行小计")
	private BigDecimal itemAmount;
}
