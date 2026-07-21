package com.erp.admin.financial.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * WB 财务报表明细查询对象
 *
 * @author system
 */
@Data
@ParameterObject
@Schema(description = "WB 财务报表明细查询对象")
public class WbReportDetailQO {

	@Schema(description = "店铺ID")
	private Long shopId;

	@Schema(description = "报表周期类型：weekly-周报, daily-日报")
	private String periodType;

	@Schema(description = "报表日期范围 - 开始")
	private String rrDtStart;

	@Schema(description = "报表日期范围 - 结束")
	private String rrDtEnd;

	@Schema(description = "订单时间范围 - 开始")
	private String orderDtStart;

	@Schema(description = "订单时间范围 - 结束")
	private String orderDtEnd;

	@Schema(description = "销售时间范围 - 开始")
	private String saleDtStart;

	@Schema(description = "销售时间范围 - 结束")
	private String saleDtEnd;

	@Schema(description = "供应商操作名称")
	private String supplierOperName;

	@Schema(description = "报表ID")
	private Long realizationreportId;

	@Schema(description = "装配单ID")
	private String assemblyId;

	@Schema(description = "是否仅显示未关联订单的记录（assemblyId为0/null或关联订单不存在）")
	private Boolean unmatchedOrderOnly;

}
