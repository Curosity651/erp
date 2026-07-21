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
 * 库位调整单明细（一行 = 一个源批次 → 一个目标库位）实体。
 *
 * @author erp
 */
@Data
@TableName("wms_location_transfer_item")
@Schema(title = "库位调整单明细实体")
public class LocationTransferItem {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "库位调整单ID")
	private Long transferOrderId;

	@Schema(title = "货主（整单归属一个货主，冗余到行）")
	private Long erpTenantId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "源批次ID（wms_physical_inventory.id）")
	private Long physicalInventoryId;

	@Schema(title = "源库位编码（冗余展示，取自批次）")
	private String sourceLocationCode;

	@Schema(title = "源品质 GOOD/DAMAGED（冗余展示，取自批次）")
	private String sourceQuality;

	@Schema(title = "目标库位编码")
	private String targetLocationCode;

	@Schema(title = "目标库位分区ID（执行时按目标解析）")
	private Long targetZoneId;

	@Schema(title = "移动数量（始终为正数）")
	private Integer quantity;

	@Schema(title = "是否落库后置为良品（退货区→标准区为1）")
	private Integer toGood;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
