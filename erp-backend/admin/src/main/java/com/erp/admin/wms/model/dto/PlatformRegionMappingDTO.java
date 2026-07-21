package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(title = "平台区域映射数据传输对象")
public class PlatformRegionMappingDTO {

    @NotBlank(message = "平台不能为空")
    @Schema(title = "平台标识")
    private String platform;

    @Schema(title = "区域ID，为空时表示清除映射")
    private Long regionId;
}
