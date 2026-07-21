package com.erp.admin.platform;

import com.erp.admin.platform.ozon.OzonResponseHandler;
import com.erp.admin.platform.rate.RateLimitRuleManager;
import com.erp.admin.platform.wildberries.WbResponseHandler;
import com.erp.admin.platform.yandex.YandexResponseHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 平台 HTTP 基础设施配置
 * <p>
 * 集中管理平台层所有 HTTP 相关的 Bean：
 * <ul>
 *   <li>OkHttpClient：平台共享的 HTTP 客户端和文件下载客户端</li>
 *   <li>ObjectMapper：平台统一的 JSON 序列化/反序列化配置</li>
 *   <li>ResponseHandler：各平台的响应处理策略</li>
 *   <li>PlatformHttpExecutor：各平台的 HTTP 执行器实例</li>
 * </ul>
 */
@Configuration
public class PlatformHttpConfig {

    private static final String USER_AGENT = "erp-platform-adapter/1.0";

    // ==================== OkHttpClient ====================

    /**
     * 平台共享 OkHttpClient
     * <p>
     * 三个平台共享同一连接池（OkHttp 按 host 隔离连接，不同平台天然独立）。
     */
    @Bean("platformOkHttpClient")
    public OkHttpClient platformOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(HttpTimeoutConfig.DEFAULT_CONNECT_TIMEOUT)
                .readTimeout(HttpTimeoutConfig.DEFAULT_READ_TIMEOUT)
                .writeTimeout(HttpTimeoutConfig.DEFAULT_WRITE_TIMEOUT)
                .connectionPool(new ConnectionPool(20, 3, TimeUnit.MINUTES))
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("User-Agent", USER_AGENT)
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    /**
     * 文件下载客户端（60s 读取超时，共享连接池）
     */
    @Bean("platformFileDownloadClient")
    public OkHttpClient platformFileDownloadClient(
            @Qualifier("platformOkHttpClient") OkHttpClient base) {
        return OkHttpHelper.createFileDownloadClient(base);
    }

    // ==================== ObjectMapper ====================

    /**
     * 平台统一 ObjectMapper
     * <p>
     * 三个平台的 Client（请求序列化）和 HttpExecutor（响应反序列化）共用此实例，
     * 避免配置不一致导致的序列化行为差异。
     */
    @Bean("platformObjectMapper")
    public ObjectMapper platformObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // ==================== ResponseHandler ====================

    @Bean
    public WbResponseHandler wbResponseHandler(RateLimitRuleManager rateLimitManager) {
        return new WbResponseHandler(rateLimitManager);
    }

    @Bean
    public OzonResponseHandler ozonResponseHandler(
            @Qualifier("platformObjectMapper") ObjectMapper objectMapper) {
        return new OzonResponseHandler(objectMapper);
    }

    @Bean
    public YandexResponseHandler yandexResponseHandler(
            @Qualifier("platformObjectMapper") ObjectMapper objectMapper) {
        return new YandexResponseHandler(objectMapper);
    }

    // ==================== PlatformHttpExecutor ====================

    @Bean("wbHttpExecutor")
    public PlatformHttpExecutor wbHttpExecutor(
            @Qualifier("platformOkHttpClient") OkHttpClient client,
            @Qualifier("platformFileDownloadClient") OkHttpClient fileClient,
            @Qualifier("platformObjectMapper") ObjectMapper objectMapper,
            RateLimitRuleManager rateLimitManager,
            WbResponseHandler responseHandler) {
        // WB 自定义退避：优先使用平台返回的 Retry-After，否则线性退避
        RetryDelayCalculator wbDelay = (attempts, ex) -> {
            if (ex.getRetryAfterMs() > 0) {
                return ex.getRetryAfterMs();
            }
            return 120L * attempts + OkHttpHelper.jitter();
        };
        return new PlatformHttpExecutor(
                PlatformEnum.Wildberries, client, fileClient, objectMapper,
                rateLimitManager, responseHandler, wbDelay);
    }

    @Bean("ozonHttpExecutor")
    public PlatformHttpExecutor ozonHttpExecutor(
            @Qualifier("platformOkHttpClient") OkHttpClient client,
            @Qualifier("platformFileDownloadClient") OkHttpClient fileClient,
            @Qualifier("platformObjectMapper") ObjectMapper objectMapper,
            RateLimitRuleManager rateLimitManager,
            OzonResponseHandler responseHandler) {
        return new PlatformHttpExecutor(
                PlatformEnum.Ozon, client, fileClient, objectMapper,
                rateLimitManager, responseHandler,
                RetryDelayCalculator.exponential(1000L));
    }

    @Bean("yandexHttpExecutor")
    public PlatformHttpExecutor yandexHttpExecutor(
            @Qualifier("platformOkHttpClient") OkHttpClient client,
            @Qualifier("platformFileDownloadClient") OkHttpClient fileClient,
            @Qualifier("platformObjectMapper") ObjectMapper objectMapper,
            RateLimitRuleManager rateLimitManager,
            YandexResponseHandler responseHandler) {
        return new PlatformHttpExecutor(
                PlatformEnum.Yandex, client, fileClient, objectMapper,
                rateLimitManager, responseHandler,
                RetryDelayCalculator.exponential(1000L));
    }
}
