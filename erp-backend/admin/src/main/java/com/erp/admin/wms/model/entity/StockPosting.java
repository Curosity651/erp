package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存操作单实体
 *
 * @author erp
 */
@Data
@TableName("wms_stock_posting")
@Schema(title = "库存操作单实体")
public class StockPosting {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

	@Schema(title = "过账单号(幂等键)")
    private String postingNo;

    @Schema(title = "仓库ID")
    private Long warehouseId;

	@Schema(title = "过账类型")
    private String postingType;

    @Schema(title = "来源单据类型")
    private String sourceType;

    @Schema(title = "来源单据ID")
    private Long sourceId;

    @Schema(title = "区域ID（存在区域级操作时填写，否则为 0）")
    private Long regionId;

    @Schema(title = "货主维（货物主人），货主级过账溯源，可空")
    private Long erpTenantId;

	@TableField(exist = false)
	@Schema(hidden = true)
	private Boolean aggregateTenantPosting;

    @Schema(title = "服务商维（货架承租方），服务商级过账溯源，可空")
    private Long wmsTenantId;

    @Schema(title = "来源单据号")
    private String sourceNo;

	@Schema(title = "业务发生时间")
	private LocalDateTime bizTime;

    @Schema(title = "操作执行时间")
    private LocalDateTime postTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

}
