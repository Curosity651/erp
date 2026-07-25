package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 确认下架入参。
 *
 * @author erp
 */
@Data
@Schema(title = "确认下架入参")
public class PickDTO {

    @NotNull(message = "出库单ID不能为空")
    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @Schema(title = "兼容字段；新流程固定为 SINGLE")
    private String pickMode;

    @NotNull(message = "拣货员不能为空")
    @Schema(title = "拣货员用户ID")
    private Long pickerId;

}
