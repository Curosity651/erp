package com.erp.admin.platform.ozon.model.request.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ozon 商品详情列表请求
 * <p>
 * API: POST /v3/product/info/list
 * <p>
 * 通过 offer_id、product_id 或 sku 获取商品详细信息（包含 sku）。
 * 一次请求最多传入 1000 个标识符。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OzonProductInfoListRequest {

    /**
     * 卖家商品编码列表
     */
    @JsonProperty("offer_id")
    private List<String> offerIds;

    /**
     * 商品 ID 列表
     */
    @JsonProperty("product_id")
    private List<Long> productIds;

    /**
     * Ozon SKU 列表
     */
    @JsonProperty("sku")
    private List<Long> skus;

}
