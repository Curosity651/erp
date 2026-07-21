package com.erp.admin.platform;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OkHttp 公共工具类
 * <p>
 * 提供平台 HTTP 执行器的公共方法，避免代码重复
 *
 * @author system
 */
public final class OkHttpHelper {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private OkHttpHelper() {
        // 工具类，禁止实例化
    }

    // ==================== OkHttpClient 相关 ====================

    /**
     * 创建文件下载客户端（60s 读取超时）
     * <p>
     * 根据 OkHttp 最佳实践，newBuilder() 会共享连接池，是轻量级操作
     *
     * @param baseClient 基础客户端
     * @return 文件下载客户端
     */
    public static OkHttpClient createFileDownloadClient(OkHttpClient baseClient) {
        return baseClient.newBuilder()
                .readTimeout(HttpTimeoutConfig.FILE_DOWNLOAD_READ_TIMEOUT)
                .build();
    }

    /**
     * 获取带自定义超时的 OkHttpClient
     *
     * @param baseClient       基础客户端
     * @param fileDownloadClient 文件下载客户端（60s 超时）
     * @param readTimeout      自定义读取超时（为 null 时返回基础客户端）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient getClientWithTimeout(OkHttpClient baseClient, 
            OkHttpClient fileDownloadClient, Duration readTimeout) {
        if (readTimeout == null || readTimeout.equals(HttpTimeoutConfig.DEFAULT_READ_TIMEOUT)) {
            return baseClient;
        }
        if (readTimeout.equals(HttpTimeoutConfig.FILE_DOWNLOAD_READ_TIMEOUT)) {
            return fileDownloadClient;
        }
        // 其他自定义超时，使用 newBuilder() 创建（共享连接池）
        return baseClient.newBuilder()
                .readTimeout(readTimeout)
				.protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
    }

    // ==================== Request 构建 ====================

    /**
     * 构建 OkHttp Request
     *
     * @param method  HTTP 方法
     * @param uri     请求 URI
     * @param headers 请求头
     * @param body    请求体（可为 null）
     * @return Request 对象
     */
    public static Request buildRequest(HttpMethod method, URI uri, HttpHeaders headers, String body) {
        Request.Builder builder = new Request.Builder().url(uri.toString());

        // 添加请求头
        if (headers != null) {
            headers.forEach((name, values) -> {
                for (String value : values) {
                    builder.addHeader(name, value);
                }
            });
        }

        // 设置请求方法和请求体
        RequestBody requestBody = null;
        if (body != null) {
            requestBody = RequestBody.create(body, JSON_MEDIA_TYPE);
        }

        switch (method.name()) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "PUT":
                builder.put(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "DELETE":
                if (requestBody != null) {
                    builder.delete(requestBody);
                } else {
                    builder.delete();
                }
                break;
            case "PATCH":
                builder.patch(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            default:
                builder.method(method.name(), requestBody);
        }

        return builder.build();
    }

    // ==================== 通用工具方法 ====================

    /**
     * 退避睡眠
     *
     * @param millis 睡眠毫秒数
     */
    public static void backoffSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 生成随机抖动（50-120ms）
     *
     * @return 随机抖动值
     */
    public static int jitter() {
        return ThreadLocalRandom.current().nextInt(50, 120);
    }

    /**
     * 截断字符串（用于日志）
     *
     * @param s      原字符串
     * @param maxLen 最大长度
     * @return 截断后的字符串
     */
    public static String abbreviate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

}
