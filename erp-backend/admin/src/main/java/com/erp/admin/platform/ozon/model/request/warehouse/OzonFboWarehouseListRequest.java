package com.erp.admin.platform.ozon.model.request.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Ozon FBO 仓库列表请求
 * <p>
 * API: POST /v1/warehouse/fbo/list
 * <p>
 * 用于获取 FBO 仓库发货点列表（cross-docking 和 direct supplies）
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OzonFboWarehouseListRequest {

    /**
     * 供应类型过滤
     * - CREATE_TYPE_CROSSDOCK: 交叉对接
     * - CREATE_TYPE_DIRECT: 直供
     */
    @JsonProperty("filter_by_supply_type")
    private List<String> filterBySupplyType;

    /**
     * 按仓库名称搜索（最少4个字符）
     * 不传则返回所有仓库
     */
    @JsonProperty("search")
    private String search;

}
