package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(title = "区域数据传输对象")
public class RegionDTO {

    @Schema(title = "主键ID (编辑时必填)")
    private Long id;

    @NotBlank(message = "区域编码不能为空")
    @Schema(title = "区域编码")
    private String regionCode;

    @NotBlank(message = "区域名称不能为空")
    @Schema(title = "区域名称")
    private String regionName;

    @NotNull(message = "状态不能为空")
    @Schema(title = "状态: 1-启用 / 0-停用")
    private Integer status;

    @Schema(title = "备注")
    private String remark;
}
