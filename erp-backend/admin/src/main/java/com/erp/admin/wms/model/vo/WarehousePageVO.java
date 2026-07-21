package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "仓库分页视图对象")
public class WarehousePageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "仓库编码")
	private String warehouseCode;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "仓库类型")
	private String warehouseType;

	@Schema(title = "所属区域ID")
	private Long regionId;

	@Schema(title = "所属区域名称")
	private String regionName;

	@Schema(title = "关联平台")
	private String platform;

	@Schema(title = "平台仓库ID")
	private String platformWarehouseId;

	@Schema(title = "状态: 1-启用 / 0-停用")
	private Integer status;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "详细地址")
	private String address;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}