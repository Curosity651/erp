package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ozon FBO 订单响应模型
 * <p>
 * FBO (Fulfillment by Ozon) 订单由 Ozon 仓库发货
 * <p>
 * API: POST /v2/posting/fbo/list
 * API: POST /v2/posting/fbo/get
 *
 * @author system
 */
@Data
public class OzonFboPosting {

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
     */
    @JsonProperty("substatus")
    private String substatus;

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
     * 物流追踪号
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;

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
     * 分析数据（包含仓库信息）
     */
    @JsonProperty("analytics_data")
    private OzonAnalyticsData analyticsData;

    /**
     * 是否为快递订单
     */
    @JsonProperty("is_express")
    private Boolean isExpress;
}
