package com.erp.admin.order.service.label;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 面单打印策略配置
 * <p>
 * 负责自动收集所有 {@link LabelPrintStrategy} 实现并构建策略注册表。
 * <p>
 * 新平台接入时，只需实现 {@link LabelPrintStrategy} 接口并添加 @Component 注解，
 * Spring 会自动注入到此配置中，无需手动注册。
 *
 * @author system
 * @see LabelPrintStrategy
 * @see LabelPrintOrchestrator
 */
@Configuration
public class LabelPrintConfig {

    /**
     * 构建策略注册表
     * <p>
     * Spring 自动收集所有 {@link LabelPrintStrategy} 实现，
     * 以平台代码为 key 构建映射表，供 {@link LabelPrintOrchestrator} 使用。
     *
     * @param strategies 所有 LabelPrintStrategy 实现（Spring 自动注入）
     * @return 平台代码 -> 策略实现 的映射
     */
    @Bean
    public Map<String, LabelPrintStrategy> labelPrintStrategyMap(
            List<LabelPrintStrategy> strategies) {

        return strategies.stream()
                .collect(Collectors.toMap(
                        LabelPrintStrategy::getPlatformCode,
                        strategy -> strategy,
                        (existing, replacement) -> {
                            // 如果存在重复的平台代码，抛出异常
                            throw new IllegalStateException(
                                    "重复的平台策略: " + existing.getPlatformCode());
                        }
                ));
    }
}
