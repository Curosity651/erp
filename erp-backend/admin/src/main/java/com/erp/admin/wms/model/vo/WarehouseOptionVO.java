package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仓库下拉选项视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "仓库下拉选项")
public class WarehouseOptionVO {

	@Schema(title = "仓库ID")
	private Long id;

	@Schema(title = "仓库编码")
	private String warehouseCode;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "仓库类型")
	private String warehouseType;

	@Schema(title = "所属区域ID")
	private Long regionId;

}