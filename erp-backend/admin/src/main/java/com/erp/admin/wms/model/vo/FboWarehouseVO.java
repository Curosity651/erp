package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台 FBO 仓库视图对象
 */
@Data
@Schema(title = "平台FBO仓库信息")
public class FboWarehouseVO {

	@Schema(title = "平台仓库ID")
	private String platformWarehouseId;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "是否已存在于ERP")
	private Boolean exists;

}
