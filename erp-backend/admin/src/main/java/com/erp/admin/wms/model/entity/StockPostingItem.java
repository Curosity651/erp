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
 * 库存操作单明细实体
 *
 * @author erp
 */
@Data
@TableName("wms_stock_posting_item")
@Schema(title = "库存操作单明细实体")
public class StockPostingItem {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "过账单ID")
    private Long postingId;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "区域ID（区域级操作时填写，仓库级操作时为 0）")
    private Long regionId;

    @Schema(title = "货主维（货物主人），区域库存按货主隔离 + 货主级过账溯源")
    private Long erpTenantId;

    @Schema(title = "服务商维（货架承租方），仓库/服务商级过账溯源，可空")
    private Long wmsTenantId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "库存桶: AVAILABLE/RESERVED/IN_TRANSIT/DAMAGED/SCRAP")
    private String bucket;

    @Schema(title = "方向: IN/OUT")
    private String direction;

    @Schema(title = "数量(正数)")
    private Integer quantity;

    @Schema(title = "明细备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

}
