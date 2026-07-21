package com.erp.admin.statistics.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard查询参数DTO
 *
 * @author erp
 */
@Data
@Schema(title = "Dashboard查询参数")
public class DashboardQueryDTO {

	/**
	 * 开始日期
	 */
	@Schema(title = "开始日期", description = "格式: YYYY-MM-DD", required = true)
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	/**
	 * 结束日期
	 */
	@Schema(title = "结束日期", description = "格式: YYYY-MM-DD", required = true)
	@DateTimeFormat(pattern = "yyyy-MM-dd")
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

	/**
	 * 预解析后的 platformItemId 列表（内部使用）
	 */
	@Schema(hidden = true)
	private List<String> platformItemIds;

}
