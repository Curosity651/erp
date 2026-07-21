package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * FBO 仓库导入请求
 */
@Data
@Schema(title = "FBO仓库导入请求")
public class FboWarehouseImportDTO {

	@NotBlank(message = "平台不能为空")
	@Schema(title = "平台: ozon / wildberries / yandex")
	private String platform;

	@NotEmpty(message = "仓库列表不能为空")
	@Valid
	@Schema(title = "要导入的仓库列表")
	private List<FboWarehouseItem> items;

	@Data
	public static class FboWarehouseItem {

		@NotBlank(message = "平台仓库ID不能为空")
		@Schema(title = "平台仓库ID")
		private String platformWarehouseId;

		@NotBlank(message = "仓库名称不能为空")
		@Schema(title = "仓库名称")
		private String warehouseName;

	}

}
