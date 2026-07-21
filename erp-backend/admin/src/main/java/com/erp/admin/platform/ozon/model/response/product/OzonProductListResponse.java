package com.erp.admin.platform.ozon.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon 商品列表响应
 * <p>
 * API: POST /v3/product/list
 */
@Data
public class OzonProductListResponse {

    @JsonProperty("result")
    private Result result;

    @Data
    public static class Result {

        @JsonProperty("items")
        private List<OzonProductItem> items;

        @JsonProperty("last_id")
        private String lastId;

        @JsonProperty("total")
        private Integer total;

    }

}
