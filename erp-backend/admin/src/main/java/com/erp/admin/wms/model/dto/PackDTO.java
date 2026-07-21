package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 打包入参。
 *
 * @author erp
 */
@Data
@Schema(title = "打包入参")
public class PackDTO {

    @NotNull(message = "出库单ID不能为空")
    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @NotNull(message = "打包模式不能为空")
    @Schema(title = "打包模式 BY_SKU/BY_ORDER/SECONDARY/CARTON")
    private String packMode;

    @Schema(title = "打包员(选填)")
    private String packerName;

}
