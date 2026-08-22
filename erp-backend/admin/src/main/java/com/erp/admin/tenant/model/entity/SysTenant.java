package com.erp.admin.tenant.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 租户（双层：WMS_OPERATOR 第一层 / ERP_USER 第二层）
 *
 * @author erp
 */
@Data
@TableName("sys_tenant")
@Schema(title = "租户")
public class SysTenant {

	@TableId(type = IdType.AUTO)
	@Schema(title = "租户ID")
	private Long id;

	@Schema(title = "租户编码（英文短码）")
	private String tenantCode;

	@Schema(title = "租户名称（公司名）")
	private String tenantName;

	@Schema(title = "海外仓内部SKU前缀，创建后保持稳定")
	private String warehouseSkuPrefix;

	@Schema(title = "租户类型：WMS_OPERATOR=3PL服务商 / ERP_USER=货主")
	private String tenantType;

	@Schema(title = "所属WMS服务商tenant_id（ERP_USER必填，WMS_OPERATOR为空）")
	private Long parentWmsTenantId;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "联系邮箱")
	private String contactEmail;

	@Schema(title = "状态：1-启用 / 0-停用")
	private Integer status;

	@Schema(title = "备注")
	private String remark;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "逻辑删除标识")
	private Long deleted;

}
