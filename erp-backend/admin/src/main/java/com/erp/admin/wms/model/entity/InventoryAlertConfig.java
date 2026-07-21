package com.erp.admin.wms.model.entity;

import lombok.Data;

import java.util.List;

/**
 * 库存预警配置（存储于 sys_config 表）
 *
 * @author erp
 */
@Data
public class InventoryAlertConfig {

    /** 默认安全库存 */
    private Integer safetyStock = 200;

    /** 是否启用通知 */
    private Boolean notifyEnabled = true;

    /** 预警阈值天数 */
    private Integer notifyThresholdDays = 7;

    /** 通知人员ID列表 */
    private List<Long> notifyUserIds;
}
