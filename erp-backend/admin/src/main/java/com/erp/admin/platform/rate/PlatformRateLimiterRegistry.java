package com.erp.admin.platform.rate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 简单令牌桶频控注册表（每账号维度）。
 *
 * 默认参数符合 WB FBS 频控（1 分钟 300 次、突发 20、建议最小 200ms 间隔）。
 * 可通过构造参数自定义。
 */
@Component
public class PlatformRateLimiterRegistry {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final double capacity;
    private final double tokensPerNano;

    public PlatformRateLimiterRegistry() {
        // 默认：capacity=20（突发），rate=300/min = 5/sec
        this(20.0, 5.0 / 1_000_000_000.0);
    }

    public PlatformRateLimiterRegistry(double capacity, double tokensPerNano) {
        this.capacity = capacity;
        this.tokensPerNano = tokensPerNano;
    }

    /**
     * 获取 key 对应的频控桶并申请一次令牌；如令牌不足则阻塞等待直到可用。
     */
    public void acquirePermission(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        bucket.acquire(tokensPerNano);
    }

    /**
     * 惩罚：额外扣减若干令牌（例如 409 计 5 次 => 需要额外扣 4）。
     */
    public void penalize(String key, int extraTokens) {
        if (extraTokens <= 0) {
			return;
		}
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        bucket.penalize(extraTokens);
    }

    /**
     * 更新剩余令牌数（基于服务端反馈）
     */
    public void updateRemaining(String key, long remaining) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        bucket.updateRemaining(remaining);
    }

    private static final class Bucket {
        private final double capacity;
        private double tokens;
        private long lastRefillNanos;

        private Bucket(double capacity) {
            this.capacity = capacity;
            this.tokens = capacity; // 初始满桶
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized void refill(double tokensPerNano) {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0)
                return;
            double add = elapsed * tokensPerNano;
            tokens = Math.min(capacity, tokens + add);
            lastRefillNanos = now;
        }

        private void acquire(double tokensPerNano) {
            for (;;) {
                synchronized (this) {
                    refill(tokensPerNano);
                    if (tokens >= 1.0) {
                        tokens -= 1.0;
                        // 获取到令牌后唤醒其他等待线程，尽快重新计算
                        this.notifyAll();
                        return;
                    }
                    // 计算还需等待的时间（纳秒）
                    double deficit = 1.0 - tokens;
                    long waitNanos = (long) Math.ceil(deficit / tokensPerNano);
                    long waitMillis = Math.max(1L, waitNanos / 1_000_000L); // 至少等待 1ms，避免忙等
                    try {
                        this.wait(waitMillis);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        private synchronized void penalize(int extraTokens) {
            tokens -= extraTokens;
            if (tokens < -capacity) {
                // 限制最小值，避免长时间过度欠账
                tokens = -capacity;
            }
            // 惩罚后通知等待线程重新评估令牌可用性
            this.notifyAll();
        }

        private synchronized void updateRemaining(long remaining) {
            // 将服务端返回的剩余配额与本地容量做保护映射
            double clamped = Math.max(-capacity, Math.min(capacity, remaining));
            // 只向下修正，避免因为模型差异导致本地桶突然"加满"
            if (clamped < this.tokens) {
                this.tokens = clamped;
            }
            this.lastRefillNanos = System.nanoTime();
            // 配额被服务端下调后，通知等待线程
            this.notifyAll();
        }
    }
}
