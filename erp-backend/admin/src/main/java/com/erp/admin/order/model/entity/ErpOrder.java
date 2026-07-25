package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表
 *
 * @author erp 2025-09-27 23:16:08
 */
@Data
@TableName("erp_order")
@Schema(title = "订单主表")
public class ErpOrder {

	/**
	 * 订单主键
	 */
	@TableId
	@Schema(title="订单主键")
	private Long id;
    
	/**
	 * 店铺ID (关联 shop.id)
	 */
	@Schema(title="店铺ID (关联 shop.id)")
	private Long shopId;
    
	/**
	 * 平台: wildberries|ozon
	 */
	@Schema(title="平台: wildberries|ozon")
	private String platform;
    
	/**
	 * 平台订单ID (WB: orderId, Ozon: posting_number)
	 */
	@Schema(title="平台订单ID (WB: orderId, Ozon: posting_number)")
	private String platformOrderId;
    
	/**
	 * 发货批次/履约单号 (WB: supplyId，Ozon: posting_number)
	 */
	@Schema(title="发货批次/履约单号 (WB: supplyId，Ozon: posting_number)")
	private String shipmentId;

	/**
	 * 商品总数量（SUM items.quantity）
	 */
	@Schema(title="商品总数量")
	private Integer totalQuantity;

	/**
	 * SKU种类数（COUNT items）
	 */
	@Schema(title="SKU种类数")
	private Integer skuCount;

	/**
	 * 订单商品明细（瞬态字段，不映射数据库列）
	 */
	@TableField(exist = false)
	private List<ErpOrderItem> items;

	/**
	 * 源仓库ID
	 * <p>
	 * - Wildberries: 卖家仓库ID (warehouseId)
	 * - Ozon FBS: 卖家发货仓库ID (delivery_method.warehouse_id)
	 * - Ozon FBO: Ozon 履约仓库ID (analytics_data.warehouse_id)
	 */
	@Schema(title="源仓库ID", description="WB:卖家仓库, Ozon:发货仓库")
	private String warehouseId;

	/**
	 * 发货仓库名（仅 Ozon FBS，来自 delivery_method.warehouse）
	 * <p>
	 * 该字段只用于仓型展示；是否需要生成交接单由店铺与配送方式规则决定。
	 * FBO 订单由 Ozon 仓配，raw_json 中无 delivery_method 节点，该字段为 null。
	 */
	@Schema(title="发货仓库名", description="仅 Ozon FBS，用于仓型展示")
	private String warehouseName;

	@Schema(title="Ozon配送方式ID")
	private Long deliveryMethodId;

	@Schema(title="Ozon配送方式名称")
	private String deliveryMethodName;

	/**
	 * 发货目的地仓库ID
	 * <p>
	 * - Wildberries: 配送中心/办公室ID (officeId)，商品从 warehouseId 发往 officeId
	 * - Ozon FBS: 发货仓库ID (与 warehouseId 相同)，仓库即发货点
	 * - Ozon FBO: 发货仓库ID (与 warehouseId 相同)，仓库即发货点
	 */
	@Schema(title="发货目的地仓库ID", description="WB:配送中心officeId, Ozon:同warehouseId")
	private String destinationWarehouseId;
    
	/**
	 * 履约类型 (FBS/FBO)
	 */
	@Schema(title="履约类型 (FBS/FBO)")
	private String fulfillmentType;
    
	/**
	 * 平台主状态 (WB:wbStatus, Ozon:status)
	 */
	@Schema(title="平台主状态", description = " (WB:wbStatus, Ozon:status)")
	private String platformStatus;
	
	/**
	 * 平台子状态 (WB:supplierStatus, Ozon:substatus)
	 */
	@Schema(title="平台子状态", description = " (WB:supplierStatus, Ozon:substatus)")
	private String platformSubstatus;
	    
    /**
     * Erp订单状态
	 * @see com.erp.admin.order.model.enums.ErpOrderStatusEnum
     */
    @Schema(title="Erp订单状态")
    private String erpStatus;

	/**
	 * 出库状态: NONE/ALLOCATED/COMPLETED
	 * @see com.erp.admin.order.model.enums.OutboundStatus
	 */
	@Schema(title="出库状态: NONE/ALLOCATED/COMPLETED")
	private String outboundStatus;

	/**
	 * 关联的出库单ID
	 */
	@Schema(title="关联的出库单ID")
	private Long outboundOrderId;

	/**
	 * 出库时间
	 */
	@Schema(title="出库时间")
	private LocalDateTime outboundTime;

	/**
	 * 已退货数量（订单级汇总，与 item 级别同事务双写）
	 */
	@Schema(title="已退货数量")
	private Integer returnedQuantity;

	/**
	 * 乐观锁版本号
	 */
	@Version
	@Schema(title="乐观锁版本号")
	private Integer version;

	/**
	 * 锁定标识：1 锁定 / 0 未锁定
	 */
	@Schema(title="锁定标识：1 锁定 / 0 未锁定")
	private Integer locked;

	@Schema(title="平台确认幂等状态 NONE/PROCESSING/SUCCESS/FAILED")
	private String confirmState;

	private LocalDateTime confirmStartedAt;
    
	/**
	 * 订单总金额
	 */
	@Schema(title="订单总金额")
	private BigDecimal totalAmount;

	/**
	 * 货币代码 ISO 4217 字母 (RUB, BYN, CNY, KZT, EUR, USD)
	 */
	@Schema(title="货币代码 ISO 4217 字母 (RUB, BYN, CNY, KZT, EUR, USD)")
	private String currencyCode;
	    
	/**
	 * 转换后金额（供应商国家货币）
	 */
	@Schema(title="转换后金额（供应商国家货币）")
	private BigDecimal convertedAmount;

	/**
	 * 转换后货币代码，暂时固定为 CNY
	 */
	@Schema(title="转换后货币代码")
	private String convertedCurrencyCode;

	/**
	 * 订单总金额(RUB)
	 */
	@Schema(title="订单总金额(RUB)")
	private BigDecimal totalAmountRub;

	/**
	 * 商品总价（原始币种）
	 */
	@Schema(title="商品总价（原始币种）")
	private BigDecimal productTotalAmount;

	/**
	 * 商品价格币种
	 */
	@Schema(title="商品价格币种")
	private String productCurrencyCode;

	/**
	 * 商品总价（CNY）
	 */
	@Schema(title="商品总价（CNY）")
	private BigDecimal productAmountCny;

	/**
	 * 买家备注
	 */
	@Schema(title="买家备注")
	private String comment;
    
	/**
	 * 面单数据
	 */
	@Schema(title="面单数据")
	private String labelBase64;
    
	/**
	 * 平台订单创建时间
	 */
	@Schema(title="平台订单创建时间")
	private LocalDateTime platformCreatedAt;

	@Schema(title = "平台订单创建时间（莫斯科时区）")
	private LocalDateTime platformCreatedAtMoscow;
    
	/**
	 * 最后一次同步时间
	 */
	@Schema(title="最后一次同步时间")
	private LocalDateTime syncedAt;
    
	/**
	 * 记录创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(title="记录创建时间")
	private LocalDateTime createTime;
    
	/**
	 * 记录更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title="记录更新时间")
	private LocalDateTime updateTime;

	/**
	 * 平台原始订单JSON
	 */
	@Schema(title="平台原始订单JSON")
	private String rawJson;

}
