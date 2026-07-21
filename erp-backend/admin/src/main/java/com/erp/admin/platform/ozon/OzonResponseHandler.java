package com.erp.admin.platform.ozon;

import com.erp.admin.platform.*;
import com.erp.admin.platform.ozon.model.common.OzonErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

/**
 * Ozon 响应处理策略
 * <p>
 * Ozon 特有行为：解析 OzonErrorResponse 获取平台返回的 code/message，
 * 提供比 HTTP 状态码更具体的错误信息。
 * <p>
 * 文档参考：https://docs.ozon.ru/api/seller/zh/?__rr=2&abt_att=2#tag/Errors
 */
@Slf4j
public class OzonResponseHandler implements ResponseHandler {

    private static final String PLATFORM = PlatformEnum.Ozon.code();

    private final ObjectMapper objectMapper;

    public OzonResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PlatformApiException handleError(int statusCode, String body, Headers headers, RequestContext ctx) {
        if (statusCode == 429) {
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

        // 4xx 尝试解析 Ozon 错误响应以获取更详细的信息
        OzonErrorResponse errorResponse = parseErrorResponse(body);
        return new PlatformApiException(PLATFORM,
                errorResponse != null ? errorResponse.getCode() : "HTTP_" + statusCode,
                errorResponse != null ? errorResponse.getMessage() : "HTTP 错误: " + statusCode,
                statusCode, null);
    }

    private OzonErrorResponse parseErrorResponse(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, OzonErrorResponse.class);
        } catch (Exception e) {
            log.debug("[OZON][HTTP] 无法解析错误响应: {}", e.getMessage());
            return null;
        }
    }
}
