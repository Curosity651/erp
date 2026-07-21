package com.erp.admin.platform.ozon.model.response.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon FBO 仓库信息
 * <p>
 * 来源：/v1/warehouse/fbo/list 或 /v1/analytics/stocks
 */
@Data
public class OzonFboWarehouse {

    /**
     * 仓库 ID（发货点、仓库或分拣中心的标识符）
     */
    @JsonProperty("warehouse_id")
    private Long warehouseId;

    /**
     * 仓库名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 仓库地址
     */
    @JsonProperty("address")
    private String address;

    /**
     * 仓库类型
     */
    @JsonProperty("warehouse_type")
    private String warehouseType;

}
