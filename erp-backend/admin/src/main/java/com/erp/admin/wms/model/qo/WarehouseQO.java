package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仓库查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "仓库查询对象")
public class WarehouseQO {

	@Schema(title = "仓库编码")
	private String warehouseCode;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "仓库类型")
	private String warehouseType;

	@Schema(title = "状态")
	private Integer status;

	@Schema(title = "所属区域ID")
	private Long regionId;

}