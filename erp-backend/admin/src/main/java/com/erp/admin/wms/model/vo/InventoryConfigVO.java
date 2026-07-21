package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SKU库存配置VO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU库存配置VO")
public class InventoryConfigVO {

    @Schema(title = "配置ID")
    private Long id;

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "区域名称")
    private String regionName;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "安全库存数量")
    private Integer safetyStock;

    @Schema(title = "是否启用通知")
    private Boolean notifyEnabled;

    @Schema(title = "预警阈值天数")
    private Integer notifyThresholdDays;

    @Schema(title = "预警阈值显示文本")
    private String notifyThresholdDisplay;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;
}
