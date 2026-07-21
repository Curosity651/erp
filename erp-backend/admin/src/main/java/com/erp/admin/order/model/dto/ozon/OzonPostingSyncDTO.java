package com.erp.admin.order.model.dto.ozon;

import com.erp.admin.platform.ozon.model.response.posting.OzonProduct;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ozon 订单 DTO
 * <p>
 * 用于在服务层之间传递订单数据
 * 
 * @author system
 */
@Data
@Builder
public class OzonPostingSyncDTO {
    
    /**
     * 发货单号 (作为平台订单 ID)
     */
    private String postingNumber;
    
    /**
     * Ozon 订单号
     */
    private String orderNumber;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 订单子状态
     */
    private String substatus;

    /**
     * 履约类型（FBS/FBO）
     */
    private String fulfillmentType;

    /**
     * 商品列表
     */
    private List<OzonProduct> products;
    
    /**
     * 源仓库 ID
     * <p>
     * FBS: delivery_method.warehouse_id (卖家发货仓库)
     * FBO: analytics_data.warehouse_id (Ozon 履约仓库)
     */
    private Long warehouseId;
    
    /**
     * 发货仓库 ID (与 warehouseId 相同)
     * <p>
     * Ozon 的仓库即发货点，没有两级结构
     */
    private Long destinationWarehouseId;
    
    /**
     * 仓库名称
     */
    private String warehouseName;
    
    /**
     * 配送方式 ID
     */
    private Long deliveryMethodId;
    
    /**
     * 配送方式名称
     */
    private String deliveryMethodName;

    /**
     * 配送方式（组合字段，用于存储）
     */
    private String deliveryMethod;
    
    /**
     * 物流服务商 ID
     */
    private Long tplProviderId;
    
    /**
     * 物流服务商名称
     */
    private String tplProviderName;
    
    /**
     * 订单进入处理时间
     */
    private OffsetDateTime inProcessAt;
    
    /**
     * 发货日期
     */
    private OffsetDateTime shipmentDate;
    
    /**
     * 配送日期
     */
    private OffsetDateTime deliveringDate;
    
    /**
     * 货币代码
     */
    private String currencyCode;
    
    /**
     * 商品价格（product.price）
     */
    private String productPrice;
    
    /**
     * 商品价格币种
     */
    private String productCurrencyCode;
    
    /**
     * 订单总金额
     * 从 products 聚合计算
     */
    private String totalAmount;
    
    /**
     * 客户实际支付价格（从 financial_data 获取）
     */
    private String customerPrice;
    
    /**
     * 客户支付价格币种
     */
    private String customerPriceCurrencyCode;
    
    /**
     * 财务数据 JSON
     * 序列化后的 financial_data
     */
    private String financialDataJson;

	/**
	 * 订单备注.
	 */
	private String comment;
    
    /**
     * 原始 JSON
     * 完整的订单响应 JSON
     */
    private String rawJson;
}
