// 金额格式化：currencyCode 采用数值代码（如 643=RUB, 156=CNY, 933=BYN 等）。
// totalAmount/convertedAmount 单位为“分”或“kopecks”等整数；按两位小数展示。
const CURRENCY_META: Record<string, { symbol: string; code: string; scale: number }> = {
  RUB: { symbol: '₽', code: 'RUB', scale: 2 },
  CNY: { symbol: '¥', code: 'CNY', scale: 2 },
  BYN: { symbol: 'Br', code: 'BYN', scale: 2 },
  EUR: { symbol: '€', code: 'EUR', scale: 2 },
  KZT: { symbol: '₸', code: 'KZT', scale: 2 },
  KGS: { symbol: 'с', code: 'KGS', scale: 2 },
  AMD: { symbol: '֏', code: 'AMD', scale: 2 },
  USD: { symbol: '$', code: 'USD', scale: 2 }
}

export const formatAmount = (
  amount?: string | number | null,
  currencyCode?: string,
  useSubunit = true // true=子单位(需要÷100), false=主单位(不需要÷)
) => {
  if (amount === undefined || amount === null) return '-'
  const meta = CURRENCY_META[String(currencyCode || '')] || {
    symbol: '',
    code: currencyCode || '',
    scale: 2
  }
  const num = typeof amount === 'string' ? Number(amount) : amount
  if (Number.isNaN(num)) return '-'

  // 如果使用子单位，需要除以10^scale进行转换
  const divisor = useSubunit ? Math.pow(10, meta.scale) : 1
  const value = (num / divisor).toFixed(2)

  return `${meta.symbol} ${value}`.trim()
}

/**
 * 获取货币符号
 * @param currencyCode 货币代码（如 RUB, CNY, EUR 等）
 * @returns 货币符号（如 ₽, ¥, € 等）
 */
export const getCurrencySymbol = (currencyCode?: string): string => {
  if (!currencyCode) return ''
  const meta = CURRENCY_META[currencyCode]
  return meta?.symbol || currencyCode
}
