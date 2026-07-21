package com.erp.admin.platform;

import org.springframework.http.HttpMethod;

import java.net.URI;

/**
 * HTTP 请求上下文
 * <p>
 * 在 ResponseHandler 中传递请求的元数据，
 * 避免 handler 直接依赖 OkHttp 的 Response 对象。
 */
public class RequestContext {

    private final PlatformEnum platform;
    private final String apiKey;
    private final HttpMethod method;
    private final URI uri;
    private final long cost;
    private final int attempts;

    public RequestContext(PlatformEnum platform, String apiKey, HttpMethod method,
                          URI uri, long cost, int attempts) {
        this.platform = platform;
        this.apiKey = apiKey;
        this.method = method;
        this.uri = uri;
        this.cost = cost;
        this.attempts = attempts;
    }

    public PlatformEnum getPlatform() {
        return platform;
    }

    public String getApiKey() {
        return apiKey;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
    }

    public long getCost() {
        return cost;
    }

    public int getAttempts() {
        return attempts;
    }
}
