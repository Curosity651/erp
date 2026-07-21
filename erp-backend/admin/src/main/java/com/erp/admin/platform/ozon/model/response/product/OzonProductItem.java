package com.erp.admin.platform.ozon.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 商品项
 * <p>
 * 注意：/v3/product/list 接口返回的商品项不包含 sku 字段。
 * 如需获取 sku，请使用 /v3/product/info/list 接口。
 */
@Data
public class OzonProductItem {

    /**
     * 商品 ID
     */
    @JsonProperty("product_id")
    private Long productId;

    /**
     * 卖家商品编码（offer_id）
     */
    @JsonProperty("offer_id")
    private String offerId;

    /**
     * 是否已归档
     */
    @JsonProperty("archived")
    private Boolean archived;

    /**
     * 是否有 FBO 库存
     */
    @JsonProperty("has_fbo_stocks")
    private Boolean hasFboStocks;

    /**
     * 是否有 FBS 库存
     */
    @JsonProperty("has_fbs_stocks")
    private Boolean hasFbsStocks;

    /**
     * 是否为折扣商品
     */
    @JsonProperty("is_discounted")
    private Boolean isDiscounted;

}
