package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 仓库数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "仓库数据传输对象")
public class WarehouseDTO {

	@Schema(title = "主键ID (编辑时必填)")
	private Long id;

	@NotBlank(message = "仓库编码不能为空")
	@Schema(title = "仓库编码")
	private String warehouseCode;

	@NotBlank(message = "仓库名称不能为空")
	@Schema(title = "仓库名称")
	private String warehouseName;

	@NotBlank(message = "仓库类型不能为空")
	@Schema(title = "仓库类型: OWN / FBO")
	private String warehouseType;

	@Schema(title = "所属区域ID")
	private Long regionId;

	@Schema(title = "关联平台 (FBO仓必填)")
	private String platform;

	@Schema(title = "平台仓库ID (FBO仓必填)")
	private String platformWarehouseId;

	@Schema(title = "仓库地址")
	private String address;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@NotNull(message = "状态不能为空")
	@Schema(title = "状态: 1-启用 / 0-停用")
	private Integer status;

	@Schema(title = "备注")
	private String remark;

}