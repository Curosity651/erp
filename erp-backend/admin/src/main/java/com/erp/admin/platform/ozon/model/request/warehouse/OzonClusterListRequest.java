package com.erp.admin.platform.ozon.model.request.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Ozon 集群列表请求
 * <p>
 * API: POST /v1/cluster/list
 * <p>
 * 用于获取 FBO 仓库集群及其仓库信息
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OzonClusterListRequest {

    /**
     * 集群 ID 列表（可选，不传则返回所有）
     */
    @JsonProperty("cluster_ids")
    private List<Long> clusterIds;

    /**
     * 集群类型（必填）
     * - CLUSTER_TYPE_OZON: 俄罗斯 Ozon 仓库集群
     * - CLUSTER_TYPE_CIS: 独联体国家仓库集群
     */
    @JsonProperty("cluster_type")
    private String clusterType;

}
