package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ozon 订单分页视图对象
 * <p>
 * 包含 Ozon 平台特有的仓库、物流、时间信息
 *
 * @author system
 */
@Data
@Schema(title = "Ozon 订单分页视图对象")
public class OzonOrderPageVO {

	// ==================== 基础信息 ====================
	
	@Schema(title = "订单主键")
	private Long id;
	
	@Schema(title = "平台名称", example = "Ozon")
	private String platform;
	
	@Schema(title = "店铺ID")
	private Long shopId;
	
	@Schema(title = "ERP店铺名称")
	private String erpShopName;
	
	@Schema(title = "平台订单ID (order_id)")
	private String platformOrderId;
	
	@Schema(title = "发货单号 (posting_number)")
	private String shipmentId;

	// ==================== 履约信息 ====================
	
	@Schema(title = "履约类型", allowableValues = {"FBS", "FBO"})
	private String fulfillmentType;
	
	// ==================== 仓库信息 ====================
	
	@Schema(title = "源仓库ID")
	private String warehouseId;
	
	@Schema(title = "发货仓库ID")
	private String destinationWarehouseId;
	
	@Schema(title = "仓库名称", description = "FBS: delivery_method.warehouse, FBO: analytics_data.warehouse_name")
	private String warehouseName;
	
	// ==================== 物流信息（FBS独有）====================
	
	@Schema(title = "配送方式ID", description = "仅FBS订单")
	private Long deliveryMethodId;
	
	@Schema(title = "配送方式名称", description = "仅FBS订单，如: Ozon Логистика самостоятельно, Москва")
	private String deliveryMethodName;
	
	@Schema(title = "物流服务商ID", description = "仅FBS订单")
	private Long tplProviderId;
	
	@Schema(title = "物流服务商名称", description = "仅FBS订单，如: Ozon Логистика")
	private String tplProviderName;
	
	@Schema(title = "物流追踪号")
	private String trackingNumber;
	
	// ==================== 状态信息 ====================
	
	@Schema(title = "平台主状态", description = "如: awaiting_packaging, awaiting_deliver, delivered")
	private String platformStatus;
	
	@Schema(title = "平台子状态")
	private String platformSubstatus;
	
	@Schema(title = "ERP状态")
	private String erpStatus;
	
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
	
	@Schema(title = "是否有面单")
	private Boolean hasLabel;
	
	// ==================== 时间信息 ====================
	
	@Schema(title = "平台订单创建时间（UTC）")
	private LocalDateTime platformCreatedAt;
	
	@Schema(title = "平台订单创建时间（莫斯科时区）")
	private LocalDateTime platformCreatedAtMoscow;
	
	@Schema(title = "订单进入处理时间（UTC）", description = "in_process_at")
	private OffsetDateTime inProcessAt;
	
	@Schema(title = "订单进入处理时间（莫斯科时区）", description = "in_process_at")
	private LocalDateTime inProcessAtMoscow;
	
	@Schema(title = "发货日期（UTC）", description = "shipment_date")
	private OffsetDateTime shipmentDate;
	
	@Schema(title = "发货日期（莫斯科时区）", description = "shipment_date")
	private LocalDateTime shipmentDateMoscow;
	
	@Schema(title = "配送日期（UTC）", description = "delivering_date")
	private OffsetDateTime deliveringDate;
	
	@Schema(title = "最后同步时间")
	private LocalDateTime syncedAt;
	
	@Schema(title = "记录创建时间")
	private LocalDateTime createTime;
	
	@Schema(title = "记录更新时间")
	private LocalDateTime updateTime;
	
	// ==================== 其他信息 ====================
	
	@Schema(title = "锁定标识", allowableValues = {"0", "1"}, description = "1:锁定, 0:未锁定")
	private Integer locked;

	@Schema(title = "商品明细列表")
	private List<OrderItemVO> items;
}

