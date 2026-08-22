package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(title = "平台订单包裹面单扫码确认")
public class PackageLabelScanDTO {

    @NotNull(message = "出库单不能为空")
    private Long outboundOrderId;

    @NotNull(message = "平台订单包裹不能为空")
    private Long packageId;

    @NotBlank(message = "面单扫描码不能为空")
    private String scanCode;
}
