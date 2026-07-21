package com.erp.admin.platform.yandex;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Yandex Market API 工具类
 * <p>
 * 提供 Yandex 相关的通用工具方法
 */
public final class YandexApiUtils {

	private YandexApiUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * 创建 Yandex 请求头（Api-Key 认证）
	 *
	 * @param apiKey Yandex API Key
	 * @return HTTP 请求头
	 */
	public static HttpHeaders createHeaders(String apiKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Api-Key", apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	/**
	 * 生成限流键
	 *
	 * @param apiKey Yandex API Key
	 * @return 限流键
	 */
	public static String rateKey(String apiKey) {
		return "yandex:" + apiKey;
	}

	/**
	 * 创建 URI
	 *
	 * @param uri URI 字符串
	 * @return URI 对象
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
	 * @param base   基础 URL
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

	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}
}
