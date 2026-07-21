package com.erp.admin.order.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

import java.time.LocalDateTime;

/**
 * 面单批次查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "面单批次查询对象")
@ParameterObject
public class LabelBatchQO {

	/**
	 * 平台: WILDBERRIES / OZON
	 */
	@Parameter(description = "平台: WILDBERRIES / OZON")
	private String platform;

	/**
	 * 创建时间开始
	 */
	@Parameter(description = "创建时间开始")
	private LocalDateTime createTimeStart;

	/**
	 * 创建时间结束
	 */
	@Parameter(description = "创建时间结束")
	private LocalDateTime createTimeEnd;

}
