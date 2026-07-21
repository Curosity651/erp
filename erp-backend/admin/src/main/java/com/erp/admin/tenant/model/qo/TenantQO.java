package com.erp.admin.tenant.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 租户列表查询条件（WMS 服务商 / 货主分页）。
 *
 * @author erp
 */
@Data
@Schema(title = "租户查询条件")
public class TenantQO {

	@Schema(title = "租户编码(模糊)")
	private String tenantCode;

	@Schema(title = "租户名称(模糊)")
	private String tenantName;

	@Schema(title = "状态：1-启用 / 0-停用")
	private Integer status;

}
