package com.erp.admin.order.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 货币代码转换工具
 * ISO 4217 数字代码 ↔ 字母代码
 */
public class CurrencyCodeConverter {

	private static final Map<Integer, String> NUMERIC_TO_ALPHA = new HashMap<>();

	static {
		NUMERIC_TO_ALPHA.put(643, "RUB");  // 俄罗斯卢布
		NUMERIC_TO_ALPHA.put(933, "BYN");  // 白俄罗斯卢布
		NUMERIC_TO_ALPHA.put(156, "CNY");  // 人民币
		NUMERIC_TO_ALPHA.put(398, "KZT");  // 哈萨克斯坦坚戈
		NUMERIC_TO_ALPHA.put(417, "KGS");  // 吉尔吉斯斯坦索姆
		NUMERIC_TO_ALPHA.put(860, "UZS");  // 乌兹别克斯坦索姆
		NUMERIC_TO_ALPHA.put(51, "AMD");  // 亚美尼亚德拉姆
		NUMERIC_TO_ALPHA.put(840, "USD");  // 美元
		NUMERIC_TO_ALPHA.put(981, "THB");   //
	}

	/**
	 * 转换 ISO 4217 数字代码 → 字母代码
	 *
	 * @param numericCode 数字代码（如 643）
	 * @return 字母代码（如 "RUB"），未知时返回原数字的字符串
	 */
	public static String toAlphaCode(Integer numericCode) {
		if (numericCode == null) {
			return null;
		}
		return NUMERIC_TO_ALPHA.getOrDefault(numericCode, String.valueOf(numericCode));
	}

	/**
	 * 转换金额（分 → 元）
	 *
	 * @param cents 分（如 100050）
	 * @return 元（如 1000.50）
	 */
	public static BigDecimal centsToAmount(Integer cents) {
		if (cents == null) {
			return null;
		}
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}
}