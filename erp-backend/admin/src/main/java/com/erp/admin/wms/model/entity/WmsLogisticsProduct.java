package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS 服务商物流产品：服务商定义（特性词条 + 统一单价），提供给名下货主；
 * 货主建单时选用，平台签出时按单价生成客户计费流水（链路二收入）。
 *
 * @author erp
 */
@Data
@TableName("wms_logistics_product")
@Schema(title = "物流产品")
public class WmsLogisticsProduct {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "归属WMS服务商")
    private Long wmsTenantId;

    @Schema(title = "产品名称")
    private String productName;

    @Schema(title = "产品编码")
    private String productCode;

    @Schema(title = "特性词条JSON数组(大件/小件/自提/自定义...)")
    private String tags;

    @Schema(title = "统一单价(每次使用)")
    private BigDecimal unitPrice;

    @Schema(title = "状态 1启用/0停用")
    private Integer status;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(title = "逻辑删除标识")
    private Long deleted;

}
