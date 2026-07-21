package com.erp.admin.order.util;

import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.platform.wildberries.model.response.sticker.WbSticker;
import com.erp.admin.platform.wildberries.model.response.sticker.WbStickersResponse;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 标签处理工具类
 *
 * @author system
 */
public final class LabelUtils {

	private LabelUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * 字符串转 Long，失败返回 null
	 *
	 * @param s 字符串
	 * @return Long 值，失败返回 null
	 */
	public static Long toLong(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 清理文件名组件，移除不安全字符
	 *
	 * @param input 输入字符串
	 * @return 清理后的安全字符串
	 */
	public static String sanitizeFileComponent(String input) {
		if (input == null) {
			return "";
		}
		String s = input.trim();
		if (s.isEmpty()) {
			return "";
		}
		// 替换不安全字符为横杠
		s = s.replaceAll(LabelConstants.UNSAFE_FILENAME_CHARS_REGEX, "-");
		// 将连续空白替换为单个横杠
		s = s.replaceAll(LabelConstants.MULTIPLE_WHITESPACE_REGEX, "-");
		// 避免出现重复横杠
		s = s.replaceAll(LabelConstants.MULTIPLE_DASH_REGEX, "-");
		// 限制单个片段长度
		if (s.length() > LabelConstants.MAX_FILENAME_COMPONENT_LENGTH) {
			s = s.substring(0, LabelConstants.MAX_FILENAME_COMPONENT_LENGTH);
		}
		return s;
	}

	/**
	 * 生成批次编号
	 * 格式：LB-yyyyMMddHHmmss
	 *
	 * @return 批次编号
	 */
	public static String generateBatchNo() {
		java.time.format.DateTimeFormatter formatter =
				java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		return "LB-" + java.time.LocalDateTime.now().format(formatter);
	}

	/**
	 * 从 WB API 响应中解析订单贴纸映射（强类型版本）
	 *
	 * @param response 强类型响应对象
	 * @return 订单ID -> Base64 映射
	 */
	public static Map<Long, String> parseOrderStickersTyped(
			WbStickersResponse response) {
		Map<Long, String> map = new HashMap<>();
		if (response == null || response.getStickers() == null) {
			return map;
		}

		for (WbSticker sticker : response.getStickers()) {
			if (sticker.getOrderId() != null && StringUtils.hasText(sticker.getFile())) {
				map.put(sticker.getOrderId(), sticker.getFile());
			}
		}
		return map;
	}

}

