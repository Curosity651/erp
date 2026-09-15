package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Yandex 订单分页视图对象
 */
@Data
@Schema(title = "Yandex 订单分页视图对象")
public class YdOrderPageVO {

	// ==================== 基础信息 ====================

	@Schema(title = "订单主键")
	private Long id;

	@Schema(title = "平台名称", example = "Yandex")
	private String platform;

	@Schema(title = "店铺ID")
	private Long shopId;

	@Schema(title = "ERP店铺名称")
	private String erpShopName;

	@Schema(title = "平台订单ID (orderId)")
	private String platformOrderId;

	@Schema(title = "发货单ID (shipment.id)")
	private String shipmentId;

	// ==================== 履约信息 ====================

	@Schema(title = "履约类型", example = "FBS")
	private String fulfillmentType;

	// ==================== 仓库信息 ====================

	@Schema(title = "仓库ID")
	private String warehouseId;

	// ==================== 状态信息 ====================

	@Schema(title = "平台主状态")
	private String platformStatus;

	@Schema(title = "平台子状态")
	private String platformSubstatus;

	@Schema(title = "ERP状态")
	private String erpStatus;

	@Schema(title = "货主业务状态")
	private String businessStatus;

	private Long fulfillmentOrderId;
	private String warehouseFulfillmentStatus;

	// ==================== 金额信息 ====================

	@Schema(title = "订单总金额")
	private BigDecimal totalAmount;

	@Schema(title = "货币代码", example = "RUB")
	private String currencyCode;

	@Schema(title = "转换后订单总金额")
	private BigDecimal convertedAmount;

	@Schema(title = "转换货币代码", example = "CNY")
	private String convertedCurrencyCode;

	@Schema(title = "商品总价（原始币种）")
	private BigDecimal productTotalAmount;

	@Schema(title = "商品价格币种")
	private String productCurrencyCode;

	@Schema(title = "商品总价（CNY）")
	private BigDecimal productAmountCny;

	// ==================== 面单信息 ====================

	// ==================== 时间信息 ====================

	@Schema(title = "平台订单创建时间（UTC）")
	private LocalDateTime platformCreatedAt;

	@Schema(title = "平台订单创建时间（莫斯科时区）")
	private LocalDateTime platformCreatedAtMoscow;

	@Schema(title = "最后同步时间")
	private LocalDateTime syncedAt;

	@Schema(title = "记录创建时间")
	private LocalDateTime createTime;

	@Schema(title = "记录更新时间")
	private LocalDateTime updateTime;

	// ==================== 其他信息 ====================

	@Schema(title = "锁定标识", allowableValues = {"0", "1"})
	private Integer locked;

	@Schema(title = "商品明细列表")
	private List<OrderItemVO> items;
}
