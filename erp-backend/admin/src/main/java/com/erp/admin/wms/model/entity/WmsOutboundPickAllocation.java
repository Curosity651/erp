package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出库下架 FIFO 分配（= 拣货单明细）。下架时按批次锁定 reserved_qty 生成，
 * 签出时据此扣减对应批次 quantity 并释放 reserved_qty。操作记录表，不加 deleted。
 *
 * @author erp
 */
@Data
@TableName("wms_outbound_pick_allocation")
@Schema(title = "出库下架FIFO分配")
public class WmsOutboundPickAllocation {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @Schema(title = "批次ID(wms_physical_inventory.id)")
    private Long physicalInventoryId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "库位编码")
    private String locationCode;

    @Schema(title = "批次入库日(FIFO)")
    private LocalDate inboundDate;

    @Schema(title = "批次同日次序")
    private Integer pickOrder;

    @Schema(title = "从该批次取货数(锁定量)")
    private Integer takeQty;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
