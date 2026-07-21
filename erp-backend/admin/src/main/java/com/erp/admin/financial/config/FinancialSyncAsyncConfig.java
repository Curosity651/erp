package com.erp.admin.financial.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 为财务同步任务配置专用线程池
 *
 * @author system
 */
@Configuration
@EnableAsync
@Slf4j
public class FinancialSyncAsyncConfig implements AsyncConfigurer {

    /**
     * 财务同步专用线程池
     * <p>
     * 配置说明：
     * - 核心线程数: 10 (可同时处理10个店铺)
     * - 最大线程数: 20 (高峰期最多20个店铺并发)
     * - 队列容量: 100 (最多缓存100个任务)
     * - 拒绝策略: CallerRunsPolicy (队列满时由调用线程执行)
     *
     * @return 线程池执行器
     */
    @Bean(name = "financialSyncExecutor")
    public Executor financialSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(10);
        
        // 最大线程数
        executor.setMaxPoolSize(20);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("financial-sync-");
        
        // 空闲线程存活时间(秒)
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略: 由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 关闭时最多等待时间(秒)
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("财务同步线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return financialSyncExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步任务执行异常: method={}, params={}, error={}",
                    method.getName(), params, throwable.getMessage(), throwable);
        };
    }
}
