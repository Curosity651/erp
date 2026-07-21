package com.erp.admin.platform.ozon.model.request.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ozon FBO 库存余额分析请求
 * <p>
 * API: POST /v1/analytics/stocks
 * <p>
 * 用于获取 FBO 仓库的库存余额分析数据。
 * 注意：skus 为必填字段，最多 100 个。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OzonAnalyticsStocksRequest {

    /**
     * SKU 列表（Ozon 系统的商品标识符）
     * 必填，最多 100 个
     */
    @JsonProperty("skus")
    private List<Long> skus;

    /**
     * 仓库ID过滤（可选）
     */
    @JsonProperty("warehouse_ids")
    private List<Long> warehouseIds;

    /**
     * 集群ID过滤（可选）
     */
    @JsonProperty("cluster_ids")
    private List<Long> clusterIds;

    /**
     * 商品标签过滤（可选）
     * <p>
     * 可选值：ITEM_ATTRIBUTE_NONE, ECONOM, NOVEL, DISCOUNT, FBS_RETURN, SUPER
     */
    @JsonProperty("item_tags")
    private List<String> itemTags;

    /**
     * 流转率等级过滤（可选）
     * <p>
     * 可选值：TURNOVER_GRADE_NONE, DEFICIT, POPULAR, ACTUAL, SURPLUS, NO_SALES,
     * WAS_NO_SALES, RESTRICTED_NO_SALES, COLLECTING_DATA, WAITING_FOR_SUPPLY,
     * WAS_DEFICIT, WAS_POPULAR, WAS_ACTUAL, WAS_SURPLUS
     */
    @JsonProperty("turnover_grades")
    private List<String> turnoverGrades;

}
