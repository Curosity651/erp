package com.erp.admin.tenant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开通租户请求（WMS 服务商 / 货主 共用）。同时创建租户与其管理员账号。
 *
 * @author erp
 */
@Data
@Schema(title = "开通租户请求")
public class OpenTenantDTO {

	@Schema(title = "租户编码（英文短码，全局唯一）")
	private String tenantCode;

	@Schema(title = "租户名称（公司名）")
	private String tenantName;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "联系邮箱")
	private String contactEmail;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "管理员用户名（全局唯一）")
	private String adminUsername;

	@Schema(title = "管理员初始密码（明文，后端加密存储）")
	private String adminPassword;

	@Schema(title = "管理员昵称")
	private String adminNickname;

}
