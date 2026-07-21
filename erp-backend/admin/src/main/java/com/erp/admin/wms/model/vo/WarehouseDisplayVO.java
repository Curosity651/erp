package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仓库展示信息视图对象 - 用于业务模块展示仓库基本信息
 * <p>
 * 包含仓库ID、名称、类型等展示所需的字段，
 * 通过warehouseId作为关联键，避免在业务数据中冗余存储仓库信息。
 *
 * @author erp
 */
@Data
@Schema(title = "仓库展示信息")
public class WarehouseDisplayVO {

	@Schema(title = "仓库ID")
	private Long id;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "仓库类型")
	private String warehouseType;

}
