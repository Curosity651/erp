package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流商实体
 *
 * @author erp
 */
@Data
@TableName("wms_logistics_provider")
@Schema(title = "物流商实体")
public class LogisticsProvider {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "归属货主 erp_tenant_id（数据级隔离，新建时自动盖章）")
	private Long erpTenantId;

	@Schema(title = "物流商编码")
	private String providerCode;

	@Schema(title = "物流商名称")
	private String providerName;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "联系邮箱")
	private String contactEmail;

	@Schema(title = "状态: 1-启用 / 0-停用")
	private Integer status;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新人")
	private Long updateBy;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "逻辑删除标识")
	private Long deleted;

}
