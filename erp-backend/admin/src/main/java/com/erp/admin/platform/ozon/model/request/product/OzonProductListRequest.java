package com.erp.admin.platform.ozon.model.request.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ozon 商品列表请求
 * <p>
 * API: POST /v3/product/list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OzonProductListRequest {

    /**
     * 商品过滤条件
     */
    @JsonProperty("filter")
    private Filter filter = new Filter();

    /**
     * 每页数量，最大 1000
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * 上一页最后一条记录的 ID，用于分页
     */
    @JsonProperty("last_id")
    private String lastId;

    /**
     * 商品过滤器
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {

        /**
         * 按 offer_id 过滤（卖家商品编码列表）
         */
        @JsonProperty("offer_id")
        private List<String> offerIds;

        /**
         * 按 product_id 过滤（商品 ID 列表）
         */
        @JsonProperty("product_id")
        private List<Long> productIds;

        /**
         * 商品可见性过滤
         * <p>
         * 可选值：ALL, VISIBLE, INVISIBLE, EMPTY_STOCK, NOT_MODERATED,
         * MODERATED, DISABLED, STATE_FAILED, READY_TO_SUPPLY, VALIDATION_STATE_PENDING,
         * VALIDATION_STATE_FAIL, VALIDATION_STATE_SUCCESS, TO_SUPPLY, IN_SALE,
         * REMOVED_FROM_SALE, BANNED, OVERPRICED, CRITICALLY_OVERPRICED, EMPTY_BARCODE,
         * BARCODE_EXISTS, QUARANTINE, ARCHIVED, OVERPRICED_WITH_STOCK, PARTIAL_APPROVED,
         * IMAGE_ABSENT, MODERATION_BLOCK
         */
        @JsonProperty("visibility")
        private String visibility;
    }

}
