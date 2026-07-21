package com.erp.admin.platform.wildberries;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Wildberries API 工具类
 * 提供 WB 相关的通用工具方法
 * 
 * @author system
 */
public final class WbApiUtils {

    private WbApiUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建 WB 请求头（仅 Authorization）
     * 
     * @param apiKey WB API Key
     * @return HTTP 请求头
     */
    public static HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", apiKey);
        return headers;
    }

    /**
     * 创建 WB JSON 请求头
     * 
     * @param apiKey WB API Key
     * @return HTTP 请求头（含 Content-Type）
     */
    public static HttpHeaders createJsonHeaders(String apiKey) {
        HttpHeaders headers = createHeaders(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 生成限流键
     * 
     * @param apiKey WB API Key
     * @return 限流键（格式：wildberries:apiKey）
     */
    public static String rateKey(String apiKey) {
        return "wildberries:" + apiKey;
    }

    /**
     * 确保请求体不为空（至少是 "{}"）
     * 
     * @param body 请求体
     * @return 非空的请求体
     */
    public static String ensureBody(String body) {
        return StringUtils.hasText(body) ? body : "{}";
    }

    /**
     * 创建 URI
     * 
     * @param uri URI 字符串
     * @return URI 对象
     * @throws IllegalArgumentException URI 格式错误
     */
    public static URI createUri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + uri, e);
        }
    }

    /**
     * 构建 URI（带查询参数）
     * 
     * @param base 基础 URL
     * @param params 查询参数
     * @return URI 对象
     */
    public static URI buildUri(String base, Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return createUri(base);
        }
        
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> {
            if (k != null && v != null) {
                joiner.add(urlEncode(k) + "=" + urlEncode(String.valueOf(v)));
            }
        });
        
        String full = base + (base.contains("?") ? "&" : "?") + joiner;
        return createUri(full);
    }

    /**
     * URL 编码
     * 
     * @param s 待编码字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}

