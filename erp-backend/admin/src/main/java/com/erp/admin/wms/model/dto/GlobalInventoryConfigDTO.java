package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 全局库存配置DTO
 *
 * @author erp
 */
@Data
@Schema(title = "全局库存配置DTO")
public class GlobalInventoryConfigDTO {

    @NotNull(message = "默认安全库存不能为空")
    @Min(value = 0, message = "安全库存不能为负数")
    @Schema(title = "默认安全库存")
    private Integer safetyStock;

    @Schema(title = "是否启用通知")
    private Boolean notifyEnabled;

    @NotNull(message = "预警阈值不能为空")
    @Min(value = 1, message = "预警阈值至少为1天")
    @Schema(title = "预警阈值天数")
    private Integer notifyThresholdDays;

    @Schema(title = "通知人员ID列表")
    private List<Long> notifyUserIds;
}
