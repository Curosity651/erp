package com.erp.admin.statistics.model.qo;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU排名查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "SKU排名查询对象")
public class SkuRankingQO {

	/**
	 * 开始日期
	 */
	@Schema(title = "开始日期", description = "格式: YYYY-MM-DD", requiredMode = Schema.RequiredMode.REQUIRED)
	private LocalDate startDate;

	/**
	 * 结束日期
	 */
	@Schema(title = "结束日期", description = "格式: YYYY-MM-DD", requiredMode = Schema.RequiredMode.REQUIRED)
	private LocalDate endDate;

	/**
	 * 平台筛选
	 */
	@Schema(title = "平台筛选", description = "wildberries|ozon", example = "all")
	private String platform;

	/**
	 * SKU编码列表（多选）
	 */
	@Schema(title = "SKU编码列表", description = "多选筛选")
	private List<String> skuCodes;

	/**
	 * 店铺ID列表（多选）
	 */
	@Schema(title = "店铺ID列表", description = "多选筛选")
	private List<Long> shopIds;

	/**
	 * 品类ID
	 */
	@Schema(title = "品类ID")
	private Long categoryId;

}
