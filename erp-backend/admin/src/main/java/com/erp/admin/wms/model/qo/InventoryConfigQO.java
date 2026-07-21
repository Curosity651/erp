package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU库存配置查询条件
 *
 * @author erp
 */
@Data
@Schema(title = "SKU库存配置查询条件")
public class InventoryConfigQO {

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "SKU关键字")
    private String skuKeyword;
}
