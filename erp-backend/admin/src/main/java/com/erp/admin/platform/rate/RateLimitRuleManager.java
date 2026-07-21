package com.erp.admin.platform.rate;

import com.erp.admin.platform.PlatformEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 限流规则管理器
 * <p>
 * 职责：
 * 1. 管理所有限流规则（初始化、存储）
 * 2. 根据 URL 匹配最高优先级的规则
 * 3. 为每个规则维护独立的限流器实例
 * 4. 提供统一的限流入口
 *
 * @author system
 */
@Component
@Slf4j
public class RateLimitRuleManager {

    /**
     * 所有限流规则（按优先级排序）
     */
    private final List<RateLimitRule> rules;

    /**
     * 每个规则对应的限流器实例（key: 规则名称）
     */
    private final Map<String, PlatformRateLimiterRegistry> limiters;

    /**
     * URL 路径匹配器（支持 Ant 风格通配符）
     */
    private final AntPathMatcher pathMatcher;

    public RateLimitRuleManager() {
        this.rules = initializeRules();
        this.limiters = new ConcurrentHashMap<>();
        this.pathMatcher = new AntPathMatcher();

        // 为每个规则创建独立的限流器实例
        rules.forEach(rule -> {
            PlatformRateLimiterRegistry limiter = new PlatformRateLimiterRegistry(
                    rule.getCapacity(),
                    rule.getTokensPerNano());
            limiters.put(rule.getName(), limiter);
            log.info("[限流规则] 初始化规则: name={}, pattern={}, priority={}, capacity={}, rate={}",
                    rule.getName(), rule.getUrlPattern(), rule.getPriority(),
                    rule.getCapacity(), rule.getTokensPerNano());
        });
    }

    /**
     * 初始化所有限流规则
     * <p>
     * 规则按优先级从高到低排列：
     * 1. 财务报表接口（优先级 100）：1分钟1次
     * 2. 标准 v3 接口（优先级 50）：1分钟300次
     * 3. 默认规则（优先级 0）：1分钟300次
     *
     * @return 限流规则列表
     */
    private List<RateLimitRule> initializeRules() {
        List<RateLimitRule> rules = new ArrayList<>();

        // =================================================================================================
        // Ozon Rules
        // =================================================================================================

        // Ozon 全局限制：50 RPS (每 Client ID)
        rules.add(RateLimitRule.builder()
                .name("ozon-global")
                .platform(PlatformEnum.Ozon)
                .urlPattern("**")
                .priority(0)
                .limit(50)
                .periodSeconds(1)
                .capacity(50) // 突发 50
                .tokensPerNano(50.0 / 1_000_000_000.0)
                .description("Ozon 全局限制：50 RPS")
                .build());

        // Ozon 商品草稿创建：2 RPS
        rules.add(RateLimitRule.builder()
                .name("ozon-draft-create")
                .platform(PlatformEnum.Ozon)
            .urlPattern("**/v1/draft/create")
                .priority(10)
                .limit(2)
                .periodSeconds(1)
                .capacity(2)
                .tokensPerNano(2.0 / 1_000_000_000.0)
                .description("Ozon 商品草稿创建：2 RPS")
                .build());

        // Ozon 供货时间槽：2 RPS
        rules.add(RateLimitRule.builder()
                .name("ozon-timeslot-get")
                .platform(PlatformEnum.Ozon)
                .urlPattern("**/v1/supply-order/timeslot/get")
                .priority(10)
                .limit(2)
                .periodSeconds(1)
                .capacity(2)
                .tokensPerNano(2.0 / 1_000_000_000.0)
                .description("Ozon 供货时间槽：2 RPS")
                .build());

        // Ozon FBS 发货：2 RPS
        rules.add(RateLimitRule.builder()
            .name("ozon-fbs-ship")
            .platform(PlatformEnum.Ozon)
            .urlPattern("**/v4/posting/fbs/ship")
            .priority(20)
            .limit(2)
            .periodSeconds(1)
            .capacity(2)
            .tokensPerNano(2.0 / 1_000_000_000.0)
            .description("Ozon FBS 发货：2 RPS")
            .build());

        // Ozon 包裹面单打印：1 RPS（保守设置）
        rules.add(RateLimitRule.builder()
            .name("ozon-package-label")
            .platform(PlatformEnum.Ozon)
            .urlPattern("**/v2/posting/fbs/package-label")
            .priority(20)
            .limit(1)
            .periodSeconds(1)
            .capacity(1)
            .tokensPerNano(1.0 / 1_000_000_000.0)
            .description("Ozon 面单打印：1 RPS")
            .build());

        // =================================================================================================
        // Wildberries Rules
        // =================================================================================================

        // WB Marketplace: 300/min
        rules.add(RateLimitRule.builder()
                .name("wb-marketplace")
                .platform(PlatformEnum.Wildberries)
                .host("marketplace-api.wildberries.ru")
                .urlPattern("**")
                .priority(10)
                .limit(300)
                .periodSeconds(60)
                .capacity(20) // 突发 20
                .tokensPerNano(300.0 / 60_000_000_000.0) // 5 RPS
                .penaltyTokens(10) // 409 惩罚 10
                .description("WB Marketplace: 300次/分钟")
                .build());

        // WB Common: 100/min (保守设置)
        rules.add(RateLimitRule.builder()
                .name("wb-common")
                .platform(PlatformEnum.Wildberries)
                .host("common-api.wildberries.ru")
                .urlPattern("**")
                .priority(10)
                .limit(100)
                .periodSeconds(60)
                .capacity(10)
                .tokensPerNano(100.0 / 60_000_000_000.0)
                .description("WB Common: 100次/分钟")
                .build());

        // WB Statistics: 1/min (严格)
        rules.add(RateLimitRule.builder()
                .name("wb-statistics")
                .platform(PlatformEnum.Wildberries)
                .host("statistics-api.wildberries.ru")
                .urlPattern("**")
                .priority(10)
                .limit(1)
                .periodSeconds(60)
                .capacity(1)
                .tokensPerNano(1.0 / 60_000_000_000.0)
                .description("WB Statistics: 1次/分钟")
                .build());

            // WB Statistics 报表明细：1/min（严格，接口级规则，优先级更高）
            rules.add(RateLimitRule.builder()
                .name("wb-statistics-report-detail")
                .platform(PlatformEnum.Wildberries)
                .host("statistics-api.wildberries.ru")
                .urlPattern("**/api/v5/supplier/reportDetailByPeriod")
                .priority(100)
                .limit(1)
                .periodSeconds(60)
                .capacity(1)
                .tokensPerNano(1.0 / 60_000_000_000.0)
                .description("WB 财务报表明细: 1次/分钟")
                .build());

        // =================================================================================================
        // Yandex Rules
        // =================================================================================================

        // Yandex 全局默认：100,000/hour（大多数接口：状态变更、装箱、面单等）
        rules.add(RateLimitRule.builder()
                .name("yandex-global")
                .platform(PlatformEnum.Yandex)
                .urlPattern("**")
                .priority(0)
                .limit(100000)
                .periodSeconds(3600)
                .capacity(50) // 突发 50
                .tokensPerNano(100000.0 / 3_600_000_000_000.0) // ≈27.8 RPS
                .description("Yandex 全局限制：100,000次/小时")
                .build());

        // Yandex 订单查询：10,000/hour
        rules.add(RateLimitRule.builder()
                .name("yandex-orders")
                .platform(PlatformEnum.Yandex)
                .urlPattern("**/businesses/*/orders")
                .priority(20)
                .limit(10000)
                .periodSeconds(3600)
                .capacity(10) // 突发 10
                .tokensPerNano(10000.0 / 3_600_000_000_000.0) // ≈2.8 RPS
                .description("Yandex 订单查询：10,000次/小时")
                .build());

        // Yandex 发货单相关：100/hour（非常严格！）
        rules.add(RateLimitRule.builder()
                .name("yandex-shipments")
                .platform(PlatformEnum.Yandex)
                .urlPattern("**/first-mile/shipments/**")
                .priority(30)
                .limit(100)
                .periodSeconds(3600)
                .capacity(2) // 突发 2，非常保守
                .tokensPerNano(100.0 / 3_600_000_000_000.0) // ≈0.028 RPS
                .description("Yandex 发货单：100次/小时（严格）")
                .build());

        // Yandex 店铺列表：1,000/hour
        rules.add(RateLimitRule.builder()
                .name("yandex-campaigns")
                .platform(PlatformEnum.Yandex)
                .urlPattern("**/v2/campaigns")
                .priority(20)
                .limit(1000)
                .periodSeconds(3600)
                .capacity(5)
                .tokensPerNano(1000.0 / 3_600_000_000_000.0) // ≈0.28 RPS
                .description("Yandex 店铺列表：1,000次/小时")
                .build());

        // Yandex 仓库列表：100/min
        rules.add(RateLimitRule.builder()
                .name("yandex-warehouses")
                .platform(PlatformEnum.Yandex)
                .urlPattern("**/businesses/*/warehouses")
                .priority(20)
                .limit(100)
                .periodSeconds(60)
                .capacity(5)
                .tokensPerNano(100.0 / 60_000_000_000.0) // ≈1.67 RPS
                .description("Yandex 仓库列表：100次/分钟")
                .build());

        return rules;
    }


    /**
     * 匹配适用于当前请求的所有规则
     *
     * @param platform 平台类型
     * @param url      请求 URL
     * @return 匹配的规则列表
     */
    public List<RateLimitRule> matchRules(PlatformEnum platform, String url) {
        String host = null;
        try {
            host = URI.create(url).getHost();
        } catch (Exception ignored) {
        }

        String finalHost = host;
        return rules.stream()
                .filter(rule -> rule.getPlatform() == platform)
                .filter(rule -> rule.getHost() == null || rule.getHost().equalsIgnoreCase(finalHost))
                .filter(rule -> pathMatcher.match(rule.getUrlPattern(), url))
                .sorted(Comparator.comparingInt(RateLimitRule::getPriority).reversed()) // 优先级高的在前（虽然我们返回所有，但排序有助于调试）
                .collect(Collectors.toList());
    }

    /**
     * 获取默认规则（优先级最低的规则）
     *
     * @return 默认规则
     */
    private RateLimitRule getDefaultRule() {
        return rules.stream()
                .min(Comparator.comparingInt(RateLimitRule::getPriority))
                .orElseThrow(() -> new IllegalStateException("没有配置默认限流规则"));
    }

    /**
     * 获取规则对应的限流器实例
     *
     * @param rule 限流规则
     * @return 限流器实例
     */
    public PlatformRateLimiterRegistry getLimiter(RateLimitRule rule) {
        PlatformRateLimiterRegistry limiter = limiters.get(rule.getName());
        if (limiter == null) {
            throw new IllegalStateException("规则 " + rule.getName() + " 没有对应的限流器实例");
        }
        return limiter;
    }

    /**
     * 执行限流（根据 URL 自动匹配规则）
     * <p>
     * 这是对外提供的统一限流入口，调用方无需关心具体的规则匹配逻辑
     * <p>
     * 执行流程：
     * 1. 根据 URL 匹配最高优先级的规则
     * 2. 获取规则对应的限流器实例
     * 3. 构建限流键（规则名称:apiKey）
     * 4. 调用限流器获取许可（如果令牌不足会阻塞等待）
     *
     * @param url    请求 URL
     * @param apiKey API 密钥
     */
    public void acquirePermission(PlatformEnum platform, String url, String apiKey) {
        // 匹配规则
        List<RateLimitRule> matchedRules = matchRules(platform, url);

        if (matchedRules.isEmpty()) {
            log.warn("[限流] 未找到匹配规则: platform={}, url={}", platform, url);
            return;
        }

        for (RateLimitRule rule : matchedRules) {
            // 获取限流器
            PlatformRateLimiterRegistry limiter = getLimiter(rule);

            // 构建限流键
            String key = buildKey(rule, apiKey);

            // 记录日志
            if (log.isDebugEnabled()) {
                log.debug("[限流] 申请令牌: rule={}, key={}", rule.getName(), key);
            }

            // 执行限流
            limiter.acquirePermission(key);
        }
    }

    /**
     * 构建限流键
     * <p>
     * 格式：规则名称:apiKey
     * <p>
     * 这样可以确保：
     * 1. 不同规则的限流器独立（通过规则名称区分）
     * 2. 同一规则下不同账号独立限流（通过 apiKey 区分）
     *
     * @param ruleName 规则名称
     * @param apiKey   API 密钥
     * @return 限流键
     */
    private String buildKey(RateLimitRule rule, String apiKey) {
        return rule.getName() + ":" + apiKey;
    }

    /**
     * 更新剩余令牌数
     *
     * @param platform  平台类型
     * @param url       请求 URL
     * @param apiKey    API 密钥
     * @param remaining 剩余令牌数
     */
    public void updateRemaining(PlatformEnum platform, String url, String apiKey, long remaining) {
        List<RateLimitRule> matchedRules = matchRules(platform, url);
        for (RateLimitRule rule : matchedRules) {
            PlatformRateLimiterRegistry limiter = getLimiter(rule);
            String key = buildKey(rule, apiKey);
            limiter.updateRemaining(key, remaining);
        }
    }

    /**
     * 惩罚（扣除令牌）
     *
     * @param platform 平台类型
     * @param url      请求 URL
     * @param apiKey   API 密钥
     * @param tokens   扣除的令牌数
     */
    public void penalize(PlatformEnum platform, String url, String apiKey, int tokens) {
        List<RateLimitRule> matchedRules = matchRules(platform, url);
        for (RateLimitRule rule : matchedRules) {
            PlatformRateLimiterRegistry limiter = getLimiter(rule);
            String key = buildKey(rule, apiKey);
            limiter.penalize(key, tokens);
        }
    }

    /**
     * 获取所有规则（用于调试和监控）
     *
     * @return 规则列表
     */
    public List<RateLimitRule> getAllRules() {
        return rules;
    }
}
