package com.erp.admin.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 货币工具类
 * <p>
 * 提供金额格式化等通用方法
 *
 * @author system
 */
public final class CurrencyUtils {

	private CurrencyUtils() {
		// 工具类禁止实例化
	}

	/**
	 * 格式化金额为带货币符号的字符串
	 * <p>
	 * 金额单位为分，转换为元后添加货币符号
	 *
	 * @param amount       金额（分）
	 * @param currencyCode 货币代码（如 "CNY"、"RUB"）
	 * @return 格式化后的字符串，如 "￥123.45"，null 时返回 "-"
	 */
	public static String formatCurrency(BigDecimal amount, String currencyCode) {
		if (amount == null) {
			return "-";
		}
		// 转换为元（除以100）
		BigDecimal amountInYuan = amount.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		// 添加货币符号
		String symbol = "CNY".equals(currencyCode) ? "￥" : currencyCode + " ";
		return symbol + String.format("%,.2f", amountInYuan);
	}

}
