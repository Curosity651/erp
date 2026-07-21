package com.erp.admin.platform.ozon.model.response.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon 集群列表响应
 * <p>
 * API: POST /v1/cluster/list
 */
@Data
public class OzonClusterListResponse {

    /**
     * 集群列表
     */
    @JsonProperty("clusters")
    private List<Cluster> clusters;

    @Data
    public static class Cluster {

        /**
         * 集群 ID
         */
        @JsonProperty("id")
        private Long id;

        /**
         * 集群名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 集群类型：CLUSTER_TYPE_OZON 或 CLUSTER_TYPE_CIS
         */
        @JsonProperty("type")
        private String type;

        /**
         * 物流集群列表
         */
        @JsonProperty("logistic_clusters")
        private List<LogisticCluster> logisticClusters;
    }

    @Data
    public static class LogisticCluster {

        /**
         * 仓库列表
         */
        @JsonProperty("warehouses")
        private List<ClusterWarehouse> warehouses;
    }

    @Data
    public static class ClusterWarehouse {

        /**
         * 仓库 ID
         */
        @JsonProperty("warehouse_id")
        private Long warehouseId;

        /**
         * 仓库名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 仓库类型：FULL_FILLMENT, CROSS_DOCK, SORTING_CENTER 等
         */
        @JsonProperty("type")
        private String type;
    }

}
