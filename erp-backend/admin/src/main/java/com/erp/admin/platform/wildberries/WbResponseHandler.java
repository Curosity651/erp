package com.erp.admin.platform.wildberries;

import com.erp.admin.platform.*;
import com.erp.admin.platform.rate.RateLimitRuleManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

/**
 * Wildberries 响应处理策略
 * <p>
 * WB 特有行为：
 * <ul>
 *   <li>409 冲突触发限流器惩罚（penalize 10 个令牌）</li>
 *   <li>429 限流解析 X-Ratelimit-Retry 头，写入 retryAfterMs 供退避策略使用</li>
 *   <li>2xx 成功后更新 X-Ratelimit-Remaining 同步服务端剩余配额</li>
 * </ul>
 */
@Slf4j
public class WbResponseHandler implements ResponseHandler {

    private static final String PLATFORM = PlatformEnum.Wildberries.code();

    private final RateLimitRuleManager rateLimitManager;

    public WbResponseHandler(RateLimitRuleManager rateLimitManager) {
        this.rateLimitManager = rateLimitManager;
    }

    @Override
    public PlatformApiException handleError(int statusCode, String body, Headers headers, RequestContext ctx) {
        // 非 429 错误也可能携带剩余配额信息
        updateRateLimitRemaining(headers, ctx);

        if (statusCode == 429) {
            long retryAfterMs = parseRetryAfter(headers);
            return new PlatformApiException(PLATFORM, "RATE_LIMIT",
                    OkHttpHelper.abbreviate(body, 300), statusCode, retryAfterMs, null);
        }

        if (statusCode == 409) {
            rateLimitManager.penalize(PlatformEnum.Wildberries,
                    ctx.getUri().toString(), ctx.getApiKey(), 10);
            return new PlatformApiException(PLATFORM, "CONFLICT",
                    OkHttpHelper.abbreviate(body, 300), statusCode);
        }

        if (statusCode == 401 || statusCode == 403) {
            return new PlatformApiException(PLATFORM, "UNAUTHORIZED",
                    OkHttpHelper.abbreviate(body, 300), statusCode);
        }

        if (statusCode >= 500) {
            return new PlatformApiException(PLATFORM, "UPSTREAM_" + statusCode,
                    OkHttpHelper.abbreviate(body, 300), statusCode);
        }

        return new PlatformApiException(PLATFORM, "HTTP_" + statusCode,
                OkHttpHelper.abbreviate(body, 300), statusCode);
    }

    @Override
    public void onSuccess(Headers headers, RequestContext ctx) {
        updateRateLimitRemaining(headers, ctx);
    }

    /**
     * 同步服务端返回的剩余配额到本地令牌桶
     */
    private void updateRateLimitRemaining(Headers headers, RequestContext ctx) {
        try {
            String v = headers.get("X-Ratelimit-Remaining");
            if (v != null) {
                long remaining = Long.parseLong(v.trim());
                rateLimitManager.updateRemaining(
                        PlatformEnum.Wildberries, ctx.getUri().toString(), ctx.getApiKey(), remaining);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * 解析 WB 429 响应中的 X-Ratelimit-Retry 头（单位：秒 → 转为毫秒）
     */
    private long parseRetryAfter(Headers headers) {
        try {
            String v = headers.get("X-Ratelimit-Retry");
            if (v != null) {
                return Long.parseLong(v.trim()) * 1000L;
            }
        } catch (NumberFormatException ignored) {
        }
        return 0L;
    }
}
