package com.erp.admin.platform;

import com.erp.admin.platform.rate.RateLimitRuleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 统一平台 HTTP 执行器
 * <p>
 * 将三个平台（WB/Ozon/Yandex）HttpExecutor 的通用逻辑统一到一个类中。
 * 职责：统一重试循环、限流控制、JSON/字节/字符串响应解析、统一日志格式。
 * <p>
 * 差异化行为通过构造器注入的策略解决：
 * <ul>
 *   <li>{@link ResponseHandler}：各平台的错误响应解析、成功后副作用（如 WB 更新限流配额）</li>
 *   <li>{@link RetryDelayCalculator}：各平台的退避策略（WB 线性/Ozon Yandex 指数）</li>
 * </ul>
 *
 * <h3>日志规范</h3>
 * <pre>
 * INFO  -> POST /path            请求开始
 * INFO  <- POST /path 200 50ms   成功
 * WARN  <- POST /path 200 5000ms 慢请求（>{@value SLOW_REQUEST_THRESHOLD_MS}ms）
 * WARN  <- POST /path 429 50ms   错误响应
 * WARN     重试等待 1/3 delay=1s  进入退避
 * ERROR    最终失败 code=... 3/3  全部重试耗尽
 * </pre>
 */
@Slf4j
public class PlatformHttpExecutor {

    /** 超过此阈值的成功请求以 WARN 级别记录 */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000L;

    private final PlatformEnum platform;
    private final String logPrefix;
    private final OkHttpClient platformOkHttpClient;
    private final OkHttpClient fileDownloadClient;
    private final ObjectMapper objectMapper;
    private final RateLimitRuleManager rateLimitManager;
    private final ResponseHandler responseHandler;
    private final RetryDelayCalculator delayCalculator;
    private final int maxAttempts;

    public PlatformHttpExecutor(PlatformEnum platform,
                                OkHttpClient platformOkHttpClient,
                                OkHttpClient fileDownloadClient,
                                ObjectMapper objectMapper,
                                RateLimitRuleManager rateLimitManager,
                                ResponseHandler responseHandler,
                                RetryDelayCalculator delayCalculator) {
        this(platform, platformOkHttpClient, fileDownloadClient, objectMapper,
                rateLimitManager, responseHandler, delayCalculator, 3);
    }

    public PlatformHttpExecutor(PlatformEnum platform,
                                OkHttpClient platformOkHttpClient,
                                OkHttpClient fileDownloadClient,
                                ObjectMapper objectMapper,
                                RateLimitRuleManager rateLimitManager,
                                ResponseHandler responseHandler,
                                RetryDelayCalculator delayCalculator,
                                int maxAttempts) {
        this.platform = platform;
        this.logPrefix = "[" + platform.code().toUpperCase() + "][HTTP]";
        this.platformOkHttpClient = platformOkHttpClient;
        this.fileDownloadClient = fileDownloadClient;
        this.objectMapper = objectMapper;
        this.rateLimitManager = rateLimitManager;
        this.responseHandler = responseHandler;
        this.delayCalculator = delayCalculator;
        this.maxAttempts = maxAttempts;
    }

    // ==================== 公开 API ====================

    /** 执行 HTTP 请求并解析为指定类型 */
    public <T> T execute(String apiKey, HttpMethod method, URI uri,
                         HttpHeaders headers, String body, Class<T> responseType) {
        return execute(apiKey, method, uri, headers, body, responseType, null);
    }

    /** 执行 HTTP 请求并解析为指定类型（支持自定义读取超时） */
    public <T> T execute(String apiKey, HttpMethod method, URI uri,
                         HttpHeaders headers, String body, Class<T> responseType,
                         Duration readTimeout) {
        String respBody = executeForString(apiKey, method, uri, headers, body, readTimeout);
        if (!StringUtils.hasText(respBody)) {
            return null;
        }
        try {
            return objectMapper.readValue(respBody, responseType);
        } catch (Exception e) {
            throw new PlatformApiException(platform.code(), "PARSE_ERROR",
                    "解析响应失败: " + e.getMessage(), 0, e);
        }
    }

    /** 执行 HTTP 请求并解析为 List */
    public <T> List<T> executeForList(String apiKey, HttpMethod method, URI uri,
                                      HttpHeaders headers, String body, Class<T> elementType) {
        return executeForList(apiKey, method, uri, headers, body, elementType, null);
    }

    /** 执行 HTTP 请求并解析为 List（支持自定义读取超时） */
    public <T> List<T> executeForList(String apiKey, HttpMethod method, URI uri,
                                      HttpHeaders headers, String body, Class<T> elementType,
                                      Duration readTimeout) {
        String respBody = executeForString(apiKey, method, uri, headers, body, readTimeout);
        if (!StringUtils.hasText(respBody)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(respBody,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            throw new PlatformApiException(platform.code(), "PARSE_ERROR",
                    "解析 List 响应失败: " + e.getMessage(), 0, e);
        }
    }

    /** 执行 HTTP 请求并返回字节数组（用于面单 PDF 等二进制内容） */
    public byte[] executeForBytes(String apiKey, HttpMethod method, URI uri,
                                  HttpHeaders headers, String body) {
        return executeForBytes(apiKey, method, uri, headers, body, null);
    }

    /** 执行 HTTP 请求并返回字节数组（支持自定义读取超时） */
    public byte[] executeForBytes(String apiKey, HttpMethod method, URI uri,
                                  HttpHeaders headers, String body, Duration readTimeout) {
        return doExecute(apiKey, method, uri, headers, body, readTimeout,
                resp -> resp.body() != null ? resp.body().bytes() : new byte[0]);
    }

    /** 执行 HTTP 请求并返回原始字符串 */
    public String executeForString(String apiKey, HttpMethod method, URI uri,
                                   HttpHeaders headers, String body) {
        return executeForString(apiKey, method, uri, headers, body, null);
    }

    /** 执行 HTTP 请求并返回原始字符串（支持自定义读取超时） */
    public String executeForString(String apiKey, HttpMethod method, URI uri,
                                   HttpHeaders headers, String body, Duration readTimeout) {
        return doExecute(apiKey, method, uri, headers, body, readTimeout,
                resp -> resp.body() != null ? resp.body().string() : null);
    }

    // ==================== 核心重试循环 ====================

    /**
     * 统一执行流程：限流等待 → 发起请求 → 成功则返回 / 失败则委托 ResponseHandler → 可重试则退避
     *
     * @param bodyReader 成功时的响应体读取策略（string 或 bytes）
     */
    private <T> T doExecute(String apiKey, HttpMethod method, URI uri,
                            HttpHeaders headers, String body, Duration readTimeout,
                            BodyReader<T> bodyReader) {
        int attempts = 0;
        PlatformApiException lastException = null;
        long totalStartTime = System.currentTimeMillis();

        OkHttpClient client = OkHttpHelper.getClientWithTimeout(
                platformOkHttpClient, fileDownloadClient, readTimeout);

        while (attempts < maxAttempts) {
            attempts++;

            // 请求开始日志
            if (attempts == 1) {
                log.info("{} -> {} {}", logPrefix, method, uri.getPath());
            } else {
                log.info("{} -> {} {} [attempt {}/{}]", logPrefix, method, uri.getPath(), attempts, maxAttempts);
            }

            try {
                rateLimitManager.acquirePermission(platform, uri.toString(), apiKey);

                long httpStartTime = System.currentTimeMillis();
                Request request = OkHttpHelper.buildRequest(method, uri, headers, body);
                try (Response response = client.newCall(request).execute()) {
                    int code = response.code();
                    long httpCost = System.currentTimeMillis() - httpStartTime;
                    RequestContext ctx = new RequestContext(platform, apiKey, method, uri, httpCost, attempts);

                    if (code >= 200 && code < 300) {
                        logSuccess(method, uri, code, httpCost);
                        responseHandler.onSuccess(response.headers(), ctx);
                        return bodyReader.read(response);
                    }

                    String errorBody = response.body() != null ? response.body().string() : null;
                    lastException = responseHandler.handleError(code, errorBody, response.headers(), ctx);
                    logErrorResponse(method, uri, code, httpCost, lastException);
                }

            } catch (PlatformApiException e) {
                lastException = e;
            } catch (IOException e) {
                lastException = new PlatformApiException(
                        platform.code(), "NETWORK_ERROR", e.getMessage(), 0, e);
                log.warn("{} <- {} {} 网络错误 error={}", logPrefix, method, uri.getPath(), e.getMessage());
            }

            // 统一重试判断
            if (!shouldRetry(lastException, attempts)) {
                break;
            }
            long delay = delayCalculator.calculate(attempts, lastException);
            log.warn("{} 重试等待 attempt={}/{} code={} delay={}ms",
                    logPrefix, attempts, maxAttempts, lastException.getErrorCode(), delay);
            OkHttpHelper.backoffSleep(delay);
        }

        // 最终失败
        long totalCost = System.currentTimeMillis() - totalStartTime;
        log.error("{} {} {} 最终失败 code={} msg={} totalCost={}ms attempts={}",
                logPrefix, method, uri.getPath(),
                lastException != null ? lastException.getErrorCode() : "UNKNOWN",
                lastException != null ? OkHttpHelper.abbreviate(lastException.getMessage(), 200) : "",
                totalCost, attempts);

        if (lastException != null) {
            throw lastException;
        }
        throw new PlatformApiException(platform.code(), "UNKNOWN_ERROR", "未知错误", 0);
    }

    // ==================== 日志方法 ====================

    /**
     * 成功响应：正常 INFO，慢请求 WARN
     */
    private void logSuccess(HttpMethod method, URI uri, int code, long cost) {
        if (cost >= SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("{} <- {} {} {} cost={}ms [SLOW]", logPrefix, method, uri.getPath(), code, cost);
        } else {
            log.info("{} <- {} {} {} cost={}ms", logPrefix, method, uri.getPath(), code, cost);
        }
    }

    /**
     * 错误响应：WARN，包含 HTTP 状态码和平台错误码
     */
    private void logErrorResponse(HttpMethod method, URI uri, int code, long cost,
                                  PlatformApiException ex) {
        log.warn("{} <- {} {} {} code={} cost={}ms", logPrefix, method, uri.getPath(), code, ex.getErrorCode(), cost);
    }

    // ==================== 重试判断 ====================

    private boolean shouldRetry(PlatformApiException e, int attempts) {
        if (e == null || attempts >= maxAttempts) {
            return false;
        }
        int status = e.getHttpStatus();
        String code = e.getErrorCode();
        return status >= 500
                || "RATE_LIMIT".equals(code)
                || "NETWORK_ERROR".equals(code);
    }

    /** 响应体读取策略，用于区分 string 和 bytes 两种读取方式 */
    @FunctionalInterface
    private interface BodyReader<T> {
        T read(Response response) throws IOException;
    }
}
