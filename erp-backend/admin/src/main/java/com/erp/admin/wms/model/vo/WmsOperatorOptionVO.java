package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WMS 服务商下拉选项（货架分配选择被分配方用）。
 *
 * @author erp
 */
@Data
@Schema(title = "WMS服务商选项")
public class WmsOperatorOptionVO {

	@Schema(title = "服务商租户ID")
	private Long id;

	@Schema(title = "服务商名称")
	private String name;

}
