package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流商分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流商分页视图对象")
public class LogisticsProviderPageVO {

	@Schema(title = "主键ID")
	private Long id;

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

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
