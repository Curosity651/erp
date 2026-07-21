package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物流商查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流商查询对象")
public class LogisticsProviderQO {

	@Schema(title = "物流商编码")
	private String providerCode;

	@Schema(title = "物流商名称")
	private String providerName;

	@Schema(title = "状态")
	private Integer status;

}
