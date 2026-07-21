package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(title = "区域分页视图对象")
public class RegionPageVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "区域编码")
    private String regionCode;

    @Schema(title = "区域名称")
    private String regionName;

    @Schema(title = "状态: 1-启用 / 0-停用")
    private Integer status;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "关联平台列表（非 DB 字段，Service 填充）")
    private List<String> platforms;

    @Schema(title = "关联仓库数量（非 DB 字段，Service 填充）")
    private Integer warehouseCount;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "更新时间")
    private LocalDateTime updateTime;
}
