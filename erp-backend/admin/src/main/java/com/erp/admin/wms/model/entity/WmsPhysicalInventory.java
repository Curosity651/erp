package com.erp.admin.wms.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 批次级库存 SSOT（方案②，自有仓）。
 *
 * <p>
 * 唯一真源：每条 = 仓 × SKU × 入库明细 × 库位 的一个批次，承载 FIFO（inbound_date →
 * pick_order → id）、分配锁定（reserved_qty）、品质（quality）、库位发生地。 写此表后须在同事务内聚合刷新
 * wms_inventory 并写 wms_stock_flow。
 * </p>
 *
 * <p>
 * 台账表：不加 deleted（与 Inventory/StockFlow 一致）。
 * </p>
 *
 * @author erp
 */
@Data
@TableName("wms_physical_inventory")
@Schema(title = "批次级库存SSOT")
public class WmsPhysicalInventory {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "货架归属(WMS服务商)，无则0")
	private Long wmsTenantId;

	@Schema(title = "货物归属(货主)")
	private Long erpTenantId;

	@Schema(title = "仓库ID(自有仓)")
	private Long warehouseId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "入库明细ID(批次溯源)")
	private Long inboundItemId;

	@Schema(title = "FIFO主键-实际上架日")
	private LocalDate inboundDate;

	@Schema(title = "同日顺序(FIFO次序)")
	private Integer pickOrder;

	@Schema(title = "批次数量")
	private Integer quantity;

	@Schema(title = "锁定(占用)数")
	private Integer reservedQty;

	@Schema(title = "品质 GOOD良品/DAMAGED次品")
	private String quality;

	@Schema(title = "库位编码(发生地)")
	private String locationCode;

	@Schema(title = "分区ID")
	private Long zoneId;

	@Schema(title = "冗余:可分配(标准/退货区=1,不良品/暂存=0)")
	private Integer allocatable;

	@Schema(title = "是否已入集装箱隐藏存储(0否1是)：1=仍计货主可用但排除FIFO挑拣、对服务商隐藏")
	private Integer containerStored;

	@Schema(title = "入箱前原库位编码(供取回默认回原位)")
	private String originLocationCode;

	@Version
	@Schema(title = "乐观锁版本号")
	private Integer version;

	@Schema(title = "创建人ID")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新人ID")
	private Long updateBy;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
