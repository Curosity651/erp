package com.erp.admin.platform.rate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流规则定义
 * <p>
 * 用于配置不同 API 接口的限流策略，支持基于 URL 模式匹配和优先级选择
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRule {

    /**
     * 规则名称（唯一标识）
     */
    private String name;

    /**
     * URL 匹配模式（支持通配符）
     * <p>
     * 支持 Ant 风格的路径匹配：
     * - ** 匹配任意层级的路径
     * - * 匹配单层路径中的任意字符
     * <p>
     * 示例：
     * - api/v5/supplier/reportDetailByPeriod （精确匹配财务报表接口）
     * - ** /api/v3/** （匹配所有 v3 版本接口）
     * - ** （匹配所有接口，通常作为默认规则）
     */
    private String urlPattern;

    /**
     * 优先级（数字越大优先级越高）
     * <p>
     * 当一个 URL 匹配多个规则时，选择优先级最高的规则
     * <p>
     * 建议优先级分配：
     * - 100+：特殊接口（如财务报表）
     * - 50-99：标准接口
     * - 0-49：默认规则
     */
    private int priority;

    /**
     * 限流阈值（例如 300）
     * <p>
     * 配合 periodSeconds 使用，用于计算 tokensPerNano
     */
    private double limit;

    /**
     * 限流周期（秒）
     */
    private long periodSeconds;

    /**
     * 令牌桶容量（突发请求数）
     * <p>
     * 表示可以立即执行的最大请求数
     */
    private double capacity;

    /**
     * 令牌生成速率（每纳秒生成的令牌数）
     * <p>
     * 计算方式：
     * - 1分钟N次 = N / 60_000_000_000.0
     * - 1秒N次 = N / 1_000_000_000.0
     * <p>
     * 示例：
     * - 1分钟1次：1.0 / 60_000_000_000.0
     * - 1分钟300次：300.0 / 60_000_000_000.0 = 5.0 / 1_000_000_000.0
     */
    private double tokensPerNano;

    /**
     * 平台类型
     */
    private com.erp.admin.platform.PlatformEnum platform;

    /**
     * Host 匹配（可选）
     * <p>
     * 如果指定了 Host，则只有请求的 Host 匹配时才应用此规则
     */
    private String host;

    /**
     * 惩罚令牌数（可选）
     * <p>
     * 例如 Wildberries 409 错误建议扣除 10 个令牌
     */
    private int penaltyTokens;

    /**
     * 规则描述
     */
    private String description;
}
