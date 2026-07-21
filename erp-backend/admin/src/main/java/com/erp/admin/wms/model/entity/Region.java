package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_region")
@Schema(title = "区域实体")
public class Region {

    @TableId(type = IdType.AUTO)
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

    @Schema(title = "创建人")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "更新人")
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(title = "逻辑删除标识")
    private Long deleted;
}
