package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "区域查询对象")
public class RegionQO {

    @Schema(title = "区域编码")
    private String regionCode;

    @Schema(title = "区域名称")
    private String regionName;

    @Schema(title = "状态")
    private Integer status;
}
