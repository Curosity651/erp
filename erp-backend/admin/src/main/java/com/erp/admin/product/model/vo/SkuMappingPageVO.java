package com.erp.admin.product.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SKU 映射分页视图对象。
 *
 * <p>用于列表页展示基础映射关系，以及补充后的 SKU 中文名、品类名称等只读信息。</p>
 */
@Data
@Schema(title = "SKU映射分页视图对象")
public class SkuMappingPageVO {

	@Schema(title = "ID")
	private Long id;

	@Schema(title = "平台商品ID")
	private String platformItemId;

	@Schema(title = "ERP SKU编码")
	private String skuCode;

	@Schema(title = "创建人")
	private Long createBy;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@Schema(title = "SKU中文名")
	private String skuChineseName;

	@Schema(title = "品类名称")
	private String categoryName;

}
