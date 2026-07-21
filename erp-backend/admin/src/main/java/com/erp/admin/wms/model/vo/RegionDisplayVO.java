package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区域展示信息 VO
 *
 * @author erp
 */
@Data
@Schema(title = "区域展示信息")
public class RegionDisplayVO {

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "区域编码")
    private String regionCode;

    @Schema(title = "区域名称")
    private String regionName;

}
