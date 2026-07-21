package com.erp.admin.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户的租户身份（B2）。前端据此裁剪菜单、展示当前身份。
 *
 * @author erp
 */
@Data
@Schema(title = "当前租户身份")
public class TenantIdentityVO {

	@Schema(title = "身份层级：OVERSEAS_PLATFORM=海外仓平台 / WMS_OPERATOR=WMS服务商 / ERP_USER=货主")
	private String identityType;

	@Schema(title = "实例/租户ID")
	private Long tenantId;

	@Schema(title = "租户类型：OVERSEAS_PLATFORM=海外仓平台 / WMS_OPERATOR=WMS服务商 / ERP_USER=货主")
	private String tenantType;

	@Schema(title = "租户编码")
	private String tenantCode;

	@Schema(title = "租户名称")
	private String tenantName;

	@Schema(title = "是否本租户管理员")
	private Boolean admin;

}
