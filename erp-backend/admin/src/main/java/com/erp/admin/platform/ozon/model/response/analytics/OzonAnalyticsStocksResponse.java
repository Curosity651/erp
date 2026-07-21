package com.erp.admin.platform.ozon.model.response.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon FBO 库存余额分析响应
 * <p>
 * API: POST /v1/analytics/stocks
 */
@Data
public class OzonAnalyticsStocksResponse {

    /**
     * 库存项列表
     */
    @JsonProperty("items")
    private List<OzonAnalyticsStockItem> items;

}
