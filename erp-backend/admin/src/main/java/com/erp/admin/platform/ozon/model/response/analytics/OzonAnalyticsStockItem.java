package com.erp.admin.platform.ozon.model.response.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon FBO 库存项信息
 * <p>
 * 来源：/v1/analytics/stocks 响应
 */
@Data
public class OzonAnalyticsStockItem {

    /**
     * Ozon SKU（商品标识符）
     */
    @JsonProperty("sku")
    private Long sku;

    /**
     * 卖家商品ID（offer_id）
     */
    @JsonProperty("offer_id")
    private String offerId;

    /**
     * 商品名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 仓库ID
     */
    @JsonProperty("warehouse_id")
    private Long warehouseId;

    /**
     * 仓库名称
     */
    @JsonProperty("warehouse_name")
    private String warehouseName;

    /**
     * 集群ID
     */
    @JsonProperty("cluster_id")
    private Long clusterId;

    /**
     * 集群名称
     */
    @JsonProperty("cluster_name")
    private String clusterName;

    /**
     * 可售库存数量
     */
    @JsonProperty("available_stock_count")
    private Integer availableStockCount;

    /**
     * 在途库存数量（供应途中）
     */
    @JsonProperty("transit_stock_count")
    private Integer transitStockCount;

    /**
     * 准备销售的库存数量
     */
    @JsonProperty("valid_stock_count")
    private Integer validStockCount;

    /**
     * 已申请供应的库存数量
     */
    @JsonProperty("requested_stock_count")
    private Integer requestedStockCount;

    /**
     * 客户退货中的库存数量
     */
    @JsonProperty("return_from_customer_stock_count")
    private Integer returnFromCustomerStockCount;

    /**
     * 残品库存数量
     */
    @JsonProperty("stock_defect_stock_count")
    private Integer stockDefectStockCount;

    // ============ 新增字段（根据 API 文档补充）============

    /**
     * 过去28天日均销量（全集群）
     */
    @JsonProperty("ads")
    private Double ads;

    /**
     * 过去28天日均销量（当前集群）
     */
    @JsonProperty("ads_cluster")
    private Double adsCluster;

    /**
     * 库存可售天数（基于过去28天日均销量计算）
     */
    @JsonProperty("idc")
    private Double idc;

    /**
     * 无销售天数（全集群）
     */
    @JsonProperty("days_without_sales")
    private Integer daysWithoutSales;

    /**
     * 无销售天数（当前集群）
     */
    @JsonProperty("days_without_sales_cluster")
    private Integer daysWithoutSalesCluster;

    /**
     * 商品标签列表
     * <p>
     * 可选值：ITEM_ATTRIBUTE_NONE, ECONOM, NOVEL, DISCOUNT, FBS_RETURN, SUPER
     */
    @JsonProperty("item_tags")
    private List<String> itemTags;

    /**
     * 流转率等级（全集群）
     * <p>
     * 可选值：TURNOVER_GRADE_NONE, DEFICIT, POPULAR, ACTUAL, SURPLUS, NO_SALES 等
     */
    @JsonProperty("turnover_grade")
    private String turnoverGrade;

    /**
     * 流转率等级（当前集群）
     */
    @JsonProperty("turnover_grade_cluster")
    private String turnoverGradeCluster;

    /**
     * 过剩库存数量（可申请移除）
     */
    @JsonProperty("excess_stock_count")
    private Integer excessStockCount;

    /**
     * 临期库存数量
     */
    @JsonProperty("expiring_stock_count")
    private Integer expiringStockCount;

    /**
     * 检验中的库存数量
     */
    @JsonProperty("other_stock_count")
    private Integer otherStockCount;

    /**
     * 待移除库存数量（应卖家请求）
     */
    @JsonProperty("return_to_seller_stock_count")
    private Integer returnToSellerStockCount;

    /**
     * 在途残品库存数量
     */
    @JsonProperty("transit_defect_stock_count")
    private Integer transitDefectStockCount;

    /**
     * 待处理标签商品数量
     */
    @JsonProperty("waiting_docs_stock_count")
    private Integer waitingDocsStockCount;

}
