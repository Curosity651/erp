package com.erp.admin.platform;

/**
 * 重试退避策略接口
 * <p>
 * 不同平台使用不同的退避策略：
 * <ul>
 *   <li>WB：自定义（优先使用平台返回的 Retry-After，否则线性退避）</li>
 *   <li>Ozon/Yandex：指数退避</li>
 * </ul>
 */
@FunctionalInterface
public interface RetryDelayCalculator {

    /**
     * 计算第 N 次重试的等待时间
     *
     * @param attempts  当前是第几次尝试（从 1 开始）
     * @param exception 触发重试的异常（可从中读取 retryAfterMs 等信息）
     * @return 等待毫秒数
     */
    long calculate(int attempts, PlatformApiException exception);

    /**
     * 指数退避：baseDelay * 2^(attempts-1) + jitter
     * <p>
     * 示例（baseDelay=1000ms）：第 1 次 ~1050ms, 第 2 次 ~2080ms, 第 3 次 ~4070ms
     */
    static RetryDelayCalculator exponential(long baseDelayMs) {
        return (attempts, ex) -> {
            long delay = baseDelayMs * (1L << (attempts - 1));
            return delay + OkHttpHelper.jitter();
        };
    }

    /**
     * 线性退避：baseDelay * attempts + jitter
     * <p>
     * 示例（baseDelay=120ms）：第 1 次 ~170ms, 第 2 次 ~290ms, 第 3 次 ~410ms
     */
    static RetryDelayCalculator linear(long baseDelayMs) {
        return (attempts, ex) -> baseDelayMs * attempts + OkHttpHelper.jitter();
    }
}
