package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.admin.wms.model.enums.StockBucket;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体
 *
 * @author erp
 */
@Data
@TableName("wms_inventory")
@Schema(title = "库存实体")
public class Inventory {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "货架归属(WMS服务商)，无则0")
    private Long wmsTenantId;

    @Schema(title = "货物归属(货主)")
    private Long erpTenantId;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "可用库存数量")
    private Integer availableQuantity;

    @Schema(title = "占用库存数量")
    private Integer reservedQuantity;

    @Schema(title = "在途库存数量")
    private Integer inTransitQuantity;

    @Schema(title = "残品库存数量")
    private Integer damagedQuantity;

    @Schema(title = "FBO最后同步时间")
    private LocalDateTime syncTime;

	@Version
	@Schema(title = "乐观锁版本号")
	private Integer version;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 根据库存桶获取对应数量
     */
    public int getQuantityByBucket(StockBucket bucket) {
        if (bucket == null) {
            return 0;
        }
        switch (bucket) {
            case AVAILABLE:
                return availableQuantity != null ? availableQuantity : 0;
            case DAMAGED:
                return damagedQuantity != null ? damagedQuantity : 0;
            case RESERVED:
                return reservedQuantity != null ? reservedQuantity : 0;
            case IN_TRANSIT:
                return inTransitQuantity != null ? inTransitQuantity : 0;
            default:
                return 0;
        }
    }

}
