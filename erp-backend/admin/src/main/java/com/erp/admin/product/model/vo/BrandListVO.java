package com.erp.admin.product.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 品牌列表VO（用于下拉选择）
 *
 * @author ballcat
 */
@Data
@Schema(title = "品牌列表VO")
public class BrandListVO {

	@Schema(title = "品牌ID")
	private Long id;

	@Schema(title = "品牌名称")
	private String name;

	@Schema(title = "品牌编码")
	private String code;

	@Schema(title = "状态")
	private Integer status;

}
