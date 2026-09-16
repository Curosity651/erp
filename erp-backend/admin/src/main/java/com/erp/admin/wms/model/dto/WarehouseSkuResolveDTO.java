package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Schema(title = "退货全局SKU批量解析")
public class WarehouseSkuResolveDTO {

    @NotEmpty(message = "全局SKU不能为空")
    @Size(max = 500, message = "单次最多解析500个全局SKU")
    private List<@NotBlank(message = "全局SKU不能为空") String> warehouseSkuCodes;
}
