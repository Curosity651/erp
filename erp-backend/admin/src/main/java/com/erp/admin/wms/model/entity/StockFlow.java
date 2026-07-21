package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.admin.wms.model.enums.StockDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

/**
 * 库存流水实体
 * 每条流水记录一个字段的变化
 *
 * @author erp
 */
@Data
@TableName("wms_stock_flow")
@Schema(title = "库存流水实体")
public class StockFlow {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "货架归属(WMS服务商)，无则0")
    private Long wmsTenantId;

    @Schema(title = "货物归属(货主)")
    private Long erpTenantId;

    @Schema(title = "区域ID（区域级流水时填写，否则为 0）")
    private Long regionId;

    @Schema(title = "SKU编码")
    private String skuCode;

	@Schema(title = "库存桶: AVAILABLE/RESERVED/IN_TRANSIT/DAMAGED/SCRAP")
    private String bucket;

	/**
	 * 方向: IN/OUT
	 * @see StockDirection
	 */
	@Schema(title = "方向: IN/OUT")
    private String direction;

	@Schema(title = "变更数量(正数)")
    private Integer quantity;

    @Schema(title = "变动前数量")
    private Integer beforeQuantity;

    @Schema(title = "变动后数量")
    private Integer afterQuantity;

	@Schema(title = "过账单ID")
    private Long postingId;

	// posting_no 为 NOT NULL 无默认值；批次上架等无过账单的流水会传空串，
	// 全局插入策略 NOT_EMPTY 会把空串字段整列丢弃 → 触发 "Field 'posting_no' doesn't have a default value"。
	// 用 NOT_NULL 策略强制把空串也写入，满足非空约束。
	@TableField(insertStrategy = FieldStrategy.NOT_NULL)
	@Schema(title = "过账单号")
    private String postingNo;

	@Schema(title = "过账单明细ID")
	private Long postingItemId;

	@Schema(title = "过账类型")
	private String postingType;

    @Schema(title = "来源单据类型")
    private String sourceType;

    @Schema(title = "来源单据ID")
    private Long sourceId;

    @Schema(title = "来源单据号")
    private String sourceNo;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "操作人ID")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    /**
     * 获取带符号的变更数量（用于计算）
     * IN = +quantity, OUT = -quantity
     */
    public int getSignedQuantity() {
		Assert.notNull(direction, "direction is null");
        return StockDirection.IN.name().equals(direction) ? quantity : -quantity;
    }

}
