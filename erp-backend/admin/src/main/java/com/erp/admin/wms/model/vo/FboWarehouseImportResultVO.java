package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * FBO 仓库导入结果
 */
@Data
@Schema(title = "FBO仓库导入结果")
public class FboWarehouseImportResultVO {

	@Schema(title = "成功导入数量")
	private Integer successCount;

	@Schema(title = "导入的仓库列表")
	private List<ImportedWarehouse> warehouses;

	@Data
	public static class ImportedWarehouse {

		@Schema(title = "仓库编码")
		private String warehouseCode;

		@Schema(title = "仓库名称")
		private String warehouseName;

	}

}
