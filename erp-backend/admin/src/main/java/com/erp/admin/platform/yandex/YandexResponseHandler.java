package com.erp.admin.platform.yandex;

import com.erp.admin.platform.*;
import com.erp.admin.platform.yandex.model.common.YandexErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

/**
 * Yandex Market 响应处理策略
 * <p>
 * Yandex 特有行为：
 * <ul>
 *   <li>使用 HTTP 420（非标准）表示限流，而非 429</li>
 *   <li>解析 YandexErrorResponse 获取 errors 列表中的首条错误信息</li>
 * </ul>
 */
@Slf4j
public class YandexResponseHandler implements ResponseHandler {

    private static final String PLATFORM = PlatformEnum.Yandex.code();

    /** Yandex 使用 420 作为限流状态码（非标准 429） */
    private static final int YANDEX_RATE_LIMIT_STATUS = 420;

    private final ObjectMapper objectMapper;

    public YandexResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PlatformApiException handleError(int statusCode, String body, Headers headers, RequestContext ctx) {
        if (statusCode == YANDEX_RATE_LIMIT_STATUS) {
            return new PlatformApiException(PLATFORM, "RATE_LIMIT",
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

        // 4xx 尝试解析 Yandex 错误响应以获取更详细的信息
        YandexErrorResponse errorResponse = parseErrorResponse(body);
        return new PlatformApiException(PLATFORM,
                errorResponse != null ? errorResponse.getFirstErrorCode() : "HTTP_" + statusCode,
                errorResponse != null ? errorResponse.getFirstErrorMessage() : "HTTP 错误: " + statusCode,
                statusCode, null);
    }

    private YandexErrorResponse parseErrorResponse(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, YandexErrorResponse.class);
        } catch (Exception e) {
            log.debug("[YANDEX][HTTP] 无法解析错误响应: {}", e.getMessage());
            return null;
        }
    }
}
