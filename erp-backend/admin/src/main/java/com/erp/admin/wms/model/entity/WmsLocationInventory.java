package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Batch-free inventory stored at a logical warehouse location.
 */
@Data
@TableName("wms_location_inventory")
@Schema(title = "逻辑库位库存")
public class WmsLocationInventory {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long tenantId;

	private Long wmsTenantId;

	private Long erpTenantId;

	private Long warehouseId;

	private Long locationId;

	private String skuCode;

	private String quality;

	private Integer quantity;

	private Integer reservedQuantity;

	@Version
	private Integer version;

	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	private Long updateBy;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

	@TableLogic
	private Long deleted;

}
