package com.erp.admin.product.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 精简 SKU 展示信息
 */
@Data
@Schema(title = "SKU简要信息")
public class SkuBriefVO {

    /**
     * SKU 主图（可为空，前端使用占位图）
     */
    @Schema(title = "SKU 主图")
    private String mainImage;

    /**
     * SKU 名称
     */
    @Schema(title = "SKU 名称")
    private String skuName;

    /**
     * SKU 编码
     */
    @Schema(title = "SKU 编码")
    private String skuCode;

	/**
	 * SKU 编号
	 */
	@Schema(title = "SKU NO")
	private Integer skuNo;

}


