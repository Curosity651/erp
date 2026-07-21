package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * SKU库存配置DTO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU库存配置DTO")
public class InventoryConfigDTO {

    @Schema(title = "配置ID，新增时为空")
    private Long id;

    @Schema(title = "区域ID")
    private Long regionId;

    @NotBlank(message = "SKU编码不能为空")
    @Schema(title = "SKU编码")
    private String skuCode;

    @Min(value = 0, message = "安全库存不能为负数")
    @Schema(title = "安全库存数量，NULL表示使用全局配置")
    private Integer safetyStock;

    @Schema(title = "是否启用通知")
    private Boolean notifyEnabled = true;

    @Schema(title = "预警阈值天数，NULL表示使用全局配置")
    private Integer notifyThresholdDays;
}
