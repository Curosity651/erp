package com.erp.admin.platform.ozon.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon 商品详情列表响应
 * <p>
 * API: POST /v3/product/info/list
 */
@Data
public class OzonProductInfoListResponse {

    /**
     * 商品详情列表
     */
    @JsonProperty("items")
    private List<OzonProductInfoItem> items;

}
