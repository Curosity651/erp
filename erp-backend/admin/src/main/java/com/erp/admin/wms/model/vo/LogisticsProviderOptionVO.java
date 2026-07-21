package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物流商下拉选项视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流商下拉选项")
public class LogisticsProviderOptionVO {

	@Schema(title = "物流商ID")
	private Long id;

	@Schema(title = "物流商编码")
	private String providerCode;

	@Schema(title = "物流商名称")
	private String providerName;

}
