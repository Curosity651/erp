package com.erp.admin.wms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库位（C1）。平台级基础设施，无租户列；上架时由 wms_physical_inventory 双 ID 标记归属。
 *
 * @author erp
 */
@Data
@TableName("wms_location")
@Schema(title = "库位")
public class WmsLocation {

	@TableId(type = IdType.AUTO)
	@Schema(title = "库位ID")
	private Long id;

	@Schema(title = "所属仓库ID")
	private Long warehouseId;

	@Schema(title = "所属分区ID")
	private Long zoneId;

	@Schema(title = "排号(如A1)")
	private String rackNo;

	@Schema(title = "列号(1开始)")
	private Integer columnNo;

	@Schema(title = "库位编码 A1-03")
	private String locationCode;

	@Schema(title = "库位类型 BIG/SMALL")
	private String locationType;

	@Schema(title = "拣货类型 PICK/STORE")
	private String pickType;

	@Schema(title = "是否虚拟库位(0物理1虚拟)：虚拟=不占物理货架、服务商不可见、收纳积压货用")
	private Integer isVirtual;

	@Schema(title = "容量上限(立方米)")
	private BigDecimal maxVolumeCbm;

	@Schema(title = "承重上限(千克)")
	private BigDecimal maxWeightKg;

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
