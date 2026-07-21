package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ozon 订单（发货单）响应模型
 * 
 * @author system
 */
@Data
public class OzonPosting {
    
    /**
     * 发货单号
     */
    @JsonProperty("posting_number")
    private String postingNumber;
    
    /**
     * 订单 ID
     */
    @JsonProperty("order_id")
    private Long orderId;
    
    /**
     * 订单号
     */
    @JsonProperty("order_number")
    private String orderNumber;
    
    /**
     * 订单状态
     * 可选值：
     * - awaiting_packaging: 等待打包
     * - awaiting_deliver: 等待发货
     * - delivering: 配送中
     * - delivered: 已送达
     * - cancelled: 已取消
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * 订单子状态
     * 提供更详细的状态信息
     */
    @JsonProperty("substatus")
    private String substatus;
    
    /**
     * 上一个子状态
     */
    @JsonProperty("previous_substatus")
    private String previousSubstatus;
    
    /**
     * 订单进入处理时间
     * <p>
     * 格式: ISO 8601 (例如: 2025-10-13T02:19:33Z)
     * Jackson 使用 InstantDeserializer.OFFSET_DATE_TIME 自动解析
     */
    @JsonProperty("in_process_at")
    private OffsetDateTime inProcessAt;
    
    /**
     * 发货日期
     * <p>
     * 格式: ISO 8601 (例如: 2025-10-13T20:59:59Z)
     * Jackson 使用 InstantDeserializer.OFFSET_DATE_TIME 自动解析
     */
    @JsonProperty("shipment_date")
    private OffsetDateTime shipmentDate;
    
    /**
     * 配送日期
     * <p>
     * 格式: ISO 8601 (例如: 2025-10-13T20:59:59Z)
     * Jackson 使用 InstantDeserializer.OFFSET_DATE_TIME 自动解析
     */
    @JsonProperty("delivering_date")
    private OffsetDateTime deliveringDate;
    
    /**
     * 取件码验证时间
     * <p>
     * 格式: ISO 8601 (例如: 2025-10-13T02:19:33Z)
     * Jackson 使用 InstantDeserializer.OFFSET_DATE_TIME 自动解析
     */
    @JsonProperty("pickup_code_verified_at")
    private OffsetDateTime pickupCodeVerifiedAt;
    
    /**
     * 配送方式信息
     */
    @JsonProperty("delivery_method")
    private OzonDeliveryMethod deliveryMethod;
    
    /**
     * 物流追踪号
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    /**
     * 物流集成类型
     */
    @JsonProperty("tpl_integration_type")
    private String tplIntegrationType;
    
    /**
     * 配送价格
     */
    @JsonProperty("delivery_price")
    private String deliveryPrice;
    
    /**
     * 物流服务商状态
     */
    @JsonProperty("provider_status")
    private String providerStatus;
    
    /**
     * 产品列表
     */
    @JsonProperty("products")
    private List<OzonProduct> products;
    
    /**
     * 财务数据
     */
    @JsonProperty("financial_data")
    private OzonFinancialData financialData;
    
    /**
     * 取消信息
     */
    @JsonProperty("cancellation")
    private OzonCancellation cancellation;
    
    /**
     * 客户信息
     */
    @JsonProperty("customer")
    private OzonCustomer customer;
    
    /**
     * 收件人信息
     */
    @JsonProperty("addressee")
    private OzonAddressee addressee;
    
    /**
     * 条码信息
     */
    @JsonProperty("barcodes")
    private OzonBarcodes barcodes;
    
    /**
     * 分析数据
     */
    @JsonProperty("analytics_data")
    private OzonAnalyticsData analyticsData;
    
    /**
     * 是否为快递订单
     */
    @JsonProperty("is_express")
    private Boolean isExpress;
    
    /**
     * 法律信息
     */
    @JsonProperty("legal_info")
    private OzonLegalInfo legalInfo;
    
    /**
     * 经济型商品 ID
     */
    @JsonProperty("quantum_id")
    private Long quantumId;
    
    /**
     * 需求信息
     */
    @JsonProperty("requirements")
    private OzonRequirements requirements;
    
    /**
     * 关联订单信息
     */
    @JsonProperty("related_postings")
    private OzonRelatedPostings relatedPostings;
    
    /**
     * 附加数据
     */
    @JsonProperty("additional_data")
    private List<Object> additionalData;
}
