package com.erp.admin.platform;

/**
 * 平台 API 统一异常
 * <p>
 * 所有平台（WB/Ozon/Yandex）的 HTTP 调用异常都包装为此类型，
 * 携带平台标识、错误码、HTTP 状态码等上下文信息，
 * 便于上层统一处理和日志排查。
 */
public class PlatformApiException extends RuntimeException {

    private final String platform;

    private final String errorCode;

    private final int httpStatus;

    /** 平台建议的重试等待时间（毫秒），0 表示无建议 */
    private final long retryAfterMs;

    public PlatformApiException(String platform, String errorCode, String message,
                                 int httpStatus, long retryAfterMs, Throwable cause) {
        super(message, cause);
        this.platform = platform;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.retryAfterMs = retryAfterMs;
    }

    public PlatformApiException(String platform, String errorCode, String message,
                                 int httpStatus, Throwable cause) {
        this(platform, errorCode, message, httpStatus, 0L, cause);
    }

    public PlatformApiException(String platform, String errorCode, String message, int httpStatus) {
        this(platform, errorCode, message, httpStatus, 0L, null);
    }

    public String getPlatform() {
        return platform;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
