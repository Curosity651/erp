package com.erp.admin.statistics.model.qo;

import com.erp.admin.statistics.model.entity.enums.TargetType;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 销售目标 查询对象
 *
 * @author erp 2025-10-25 21:40:57
 */
@Data
@Schema(title = "销售目标查询对象")
@ParameterObject
public class SalesTargetQO {

	@Parameter(description = "目标类型")
	private TargetType targetType;

	@Parameter(description = "目标年份")
	private Integer targetYear;

	@Parameter(description = "目标月份")
	private Integer targetMonth;

}