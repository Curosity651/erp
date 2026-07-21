package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存查询条件
 *
 * @author erp
 */
@Data
@Schema(title = "库存查询条件")
public class InventoryQO {

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "SKU编码（模糊）")
    private String skuCode;

    @Schema(title = "仓库类型")
    private String warehouseType;

    @Schema(title = "库存状态（HAS_STOCK/ZERO_STOCK/NEGATIVE_STOCK）")
    private String stockStatus;

    @Schema(title = "仅显示有残品的SKU")
    private Boolean hasDamaged;

    /**
     * 数据级可见性作用域：货物归属(货主) tenant_id。由服务层按当前身份注入（货主=自己；
     * 平台超管=-1 哨兵→0 行；服务商=自身运营商 id→0 行），不由前端传入。
     */
    @Schema(hidden = true)
    private Long erpTenantId;

}
