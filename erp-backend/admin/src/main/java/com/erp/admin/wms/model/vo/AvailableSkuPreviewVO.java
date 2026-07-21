package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 可盘点SKU预览VO
 *
 * @author erp
 */
@Data
@Schema(title = "可盘点SKU预览VO")
public class AvailableSkuPreviewVO {

    @Schema(description = "SKU总数")
    private Integer totalCount;

    @Schema(description = "SKU列表（分页预览，最多显示10个）")
    private List<AvailableSkuVO> items;
}
