package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Wildberries 订单分页视图对象
 * <p>
 * 包含 Wildberries 平台特有的办公室、仓库信息
 *
 * @author system
 */
@Data
@Schema(title = "Wildberries 订单分页视图对象")
public class WbOrderPageVO {

	// ==================== 基础信息 ====================
	
	@Schema(title = "订单主键")
	private Long id;
	
	@Schema(title = "平台名称", example = "Wildberries")
	private String platform;
	
	@Schema(title = "店铺ID")
	private Long shopId;
	
	@Schema(title = "ERP店铺名称")
	private String erpShopName;
	
	@Schema(title = "平台订单ID (orderId)")
	private String platformOrderId;
	
	@Schema(title = "供货批次ID (supplyId)")
	private String shipmentId;

	// ==================== 履约信息 ====================

	@Schema(title = "履约类型", allowableValues = {"FBS", "FBO"})
	private String fulfillmentType;
	
	// ==================== 仓库信息 ====================
	
	@Schema(title = "卖家仓库ID")
	private String warehouseId;
	
	@Schema(title = "配送办公室ID (officeId)")
	private String destinationWarehouseId;
	
	@Schema(title = "配送办公室名称")
	private String destinationWarehouseName;
	
	@Schema(title = "配送办公室地址")
	private String destinationWarehouseAddress;
	
	// ==================== 状态信息 ====================
	
	@Schema(title = "平台主状态 (wbStatus)")
	private String platformStatus;
	
	@Schema(title = "平台子状态 (supplierStatus)")
	private String platformSubstatus;
	
	@Schema(title = "ERP状态")
	private String erpStatus;
	
	// ==================== 金额信息 ====================
	
	@Schema(title = "订单总金额")
	private BigDecimal totalAmount;
	
	@Schema(title = "货币代码", example = "RUB")
	private String currencyCode;
	
	@Schema(title = "转换后金额（供应商国家货币）")
	private BigDecimal convertedAmount;
	
	@Schema(title = "转换后的币种")
	private String convertedCurrencyCode;
	
	// ==================== 面单信息 ====================
	
	@Schema(title = "是否有面单")
	private Boolean hasLabel;
	
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
	
	@Schema(title = "锁定标识", allowableValues = {"0", "1"}, description = "1:锁定, 0:未锁定")
	private Integer locked;

	@Schema(title = "商品明细列表")
	private List<OrderItemVO> items;
}

