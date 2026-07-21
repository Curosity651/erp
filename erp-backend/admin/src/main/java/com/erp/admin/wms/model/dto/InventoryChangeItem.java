package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存变更项
 *
 * @author erp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "库存变更项")
public class InventoryChangeItem {

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "变更数量")
    private Integer quantity;

}
