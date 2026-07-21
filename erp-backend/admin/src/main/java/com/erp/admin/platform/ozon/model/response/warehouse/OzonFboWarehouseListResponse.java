package com.erp.admin.platform.ozon.model.response.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon FBO 仓库列表响应
 * <p>
 * API: POST /v1/warehouse/fbo/list
 */
@Data
public class OzonFboWarehouseListResponse {

    /**
     * 仓库搜索结果
     */
    @JsonProperty("search")
    private List<OzonFboWarehouse> search;

}
