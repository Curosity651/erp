package com.erp.admin.wms.model.entity;

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
 * 仓内品质分区（C1）。平台级基础设施，无租户列。
 *
 * @author erp
 */
@Data
@TableName("wms_zone")
@Schema(title = "品质分区")
public class WmsZone {

	@TableId(type = IdType.AUTO)
	@Schema(title = "分区ID")
	private Long id;

	@Schema(title = "所属仓库ID")
	private Long warehouseId;

	@Schema(title = "分区类型 STANDARD/DEFECTIVE/RETURN/TEMP")
	private String zoneType;

	@Schema(title = "分区名称")
	private String zoneName;

	@Schema(title = "可分配 1是/0否")
	private Integer allocatable;

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
