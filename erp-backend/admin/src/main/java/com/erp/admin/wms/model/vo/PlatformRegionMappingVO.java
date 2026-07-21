package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "平台区域映射视图对象")
public class PlatformRegionMappingVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "平台标识")
    private String platform;

    @Schema(title = "区域ID")
    private Long regionId;
}
