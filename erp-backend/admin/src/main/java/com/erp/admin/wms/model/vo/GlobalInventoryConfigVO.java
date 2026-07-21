package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 全局库存配置VO
 *
 * @author erp
 */
@Data
@Schema(title = "全局库存配置VO")
public class GlobalInventoryConfigVO {

    @Schema(title = "默认安全库存")
    private Integer safetyStock;

    @Schema(title = "是否启用通知")
    private Boolean notifyEnabled;

    @Schema(title = "预警阈值天数")
    private Integer notifyThresholdDays;

    @Schema(title = "通知人员ID列表")
    private List<Long> notifyUserIds;

    @Schema(title = "通知人员名称列表")
    private List<String> notifyUserNames;
}
