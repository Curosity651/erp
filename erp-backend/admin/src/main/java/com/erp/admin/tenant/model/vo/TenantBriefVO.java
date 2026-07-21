package com.erp.admin.tenant.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 租户列表项（开通管理页用）。
 *
 * @author erp
 */
@Data
@Schema(title = "租户列表项")
public class TenantBriefVO {

	@Schema(title = "租户ID")
	private Long id;

	@Schema(title = "租户编码")
	private String tenantCode;

	@Schema(title = "租户名称")
	private String tenantName;

	@Schema(title = "租户类型")
	private String tenantType;

	@Schema(title = "所属WMS服务商ID（货主才有）")
	private Long parentWmsTenantId;

	@Schema(title = "状态：1启用/0停用")
	private Integer status;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
