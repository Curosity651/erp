package com.erp.admin.platform.wildberries.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置: Wildberries 订单同步。
 * 可在 application.yml 中覆盖:
 * erp:
 *   sync:
 *     wb:
 *       enabled: true
 *       cron: "0 0/2 * * * ?"
 *       lookback-minutes: 5
 *       page-size: 100
 */
@Data
@Component
@ConfigurationProperties(prefix = "erp.sync.wb")
public class WildberriesSyncProperties {
    /** 是否启用 */
    private boolean enabled = true;
    /** cron 表达式 (供 scheduler 可选使用) */
    private String cron = "0 3/10 * * * ?";
    /** 回溯分钟数 */
    private long lookbackMinutes = 5L;
    /** 单页大小 */
    private int pageSize = 100;
}
