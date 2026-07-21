package com.erp.admin.platform;

import okhttp3.Headers;

/**
 * 平台响应处理策略接口
 * <p>
 * 各平台实现此接口来处理差异化的错误响应解析和成功后的副作用。
 * <p>
 * 设计约定：
 * <ul>
 *   <li>{@link #handleError} 返回异常对象（不直接 throw），由调用方控制 throw 时机</li>
 *   <li>{@link #onSuccess} 仅用于副作用（如 WB 更新限流配额），不得消费 response body</li>
 * </ul>
 */
public interface ResponseHandler {

    /**
     * 处理非 2xx 错误响应
     *
     * @param statusCode HTTP 状态码
     * @param body       响应体（已读取为字符串，可能为 null）
     * @param headers    响应头（用于读取限流相关 header 等）
     * @param ctx        请求上下文
     * @return 要抛出的 PlatformApiException
     */
    PlatformApiException handleError(int statusCode, String body, Headers headers, RequestContext ctx);

    /**
     * 2xx 成功后的钩子
     * <p>
     * 默认无操作。WB 覆写此方法来更新 X-Ratelimit-Remaining。
     * <p>
     * 注意：此方法调用时 response body 尚未消费，实现中不得读取 body。
     */
    default void onSuccess(Headers headers, RequestContext ctx) {
    }
}
