package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 库位改分区请求（C1）。
 *
 * @author erp
 */
@Data
@Schema(title = "库位改分区请求")
public class MoveLocationZoneDTO {

	@NotNull(message = "仓库ID不能为空")
	@Schema(title = "仓库ID")
	private Long warehouseId;

	@NotEmpty(message = "请选择库位")
	@Schema(title = "库位ID列表")
	private List<Long> locationIds;

	@NotNull(message = "目标分区不能为空")
	@Schema(title = "目标分区ID")
	private Long zoneId;

}
