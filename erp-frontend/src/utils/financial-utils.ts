import dayjs from 'dayjs'

/**
 * 格式化日期（YYYY-MM-DD）
 * @param date 日期字符串或 Date 对象
 * @returns 格式化后的日期字符串，如果为空则返回 "-"
 */
export function formatDate(date?: string | Date | null): string {
  if (!date) return '-'

  try {
    const parsed = dayjs(date)
    if (!parsed.isValid()) return '-'
    return parsed.format('YYYY-MM-DD')
  } catch {
    return '-'
  }
}

/**
 * 格式化日期时间（YYYY-MM-DD HH:mm:ss）
 * @param datetime 日期时间字符串或 Date 对象
 * @returns 格式化后的日期时间字符串，如果为空则返回 "-"
 */
export function formatDateTime(datetime?: string | Date | null): string {
  if (!datetime) return '-'

  try {
    const parsed = dayjs(datetime)
    if (!parsed.isValid()) return '-'
    return parsed.format('YYYY-MM-DD HH:mm:ss')
  } catch {
    return '-'
  }
}

/**
 * 格式化金额（带货币符号和千分位）
 * @param amount 金额数值
 * @param currencyCode 货币代码（如 RUB, CNY 等）
 * @returns 格式化后的金额字符串，如果为空则返回 "-"
 */
export function formatCurrency(amount?: number | string | null, currencyCode?: string): string {
  if (amount === undefined || amount === null || amount === '') return '-'

  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '-'

  // 货币符号映射
  const currencySymbols: Record<string, string> = {
    RUB: '₽',
    CNY: '¥',
    BYN: 'Br',
    EUR: '€',
    KZT: '₸',
    KGS: 'с',
    AMD: '֏',
    USD: '$'
  }

  currencyCode = currencyCode?.toUpperCase()
  console.log(currencyCode)
  const symbol = currencySymbols[currencyCode || ''] || currencyCode || ''

  // 格式化为千分位，保留两位小数
  const formatted = num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })

  return symbol ? `${symbol}${formatted}` : formatted
}

/**
 * 处理空值显示
 * @param value 任意值
 * @param placeholder 占位符，默认为 "-"
 * @returns 如果值为空则返回占位符，否则返回原值的字符串形式
 */
export function formatEmptyValue(value?: any, placeholder = '-'): string {
  if (value === undefined || value === null || value === '') {
    return placeholder
  }

  // 如果是字符串且只包含空白字符
  if (typeof value === 'string' && value.trim() === '') {
    return placeholder
  }

  return String(value)
}

/**
 * 格式化百分比
 * @param percent 百分比数值
 * @param decimals 小数位数，默认为 2
 * @returns 格式化后的百分比字符串，如果为空则返回 "-"
 */
export function formatPercent(percent?: number | string | null, decimals = 2): string {
  if (percent === undefined || percent === null || percent === '') return '-'

  const num = typeof percent === 'string' ? parseFloat(percent) : percent
  if (isNaN(num)) return '-'

  return `${num.toFixed(decimals)}%`
}

/**
 * 格式化布尔值为中文
 * @param value 布尔值
 * @returns "是" 或 "否"，如果为空则返回 "-"
 */
export function formatBoolean(value?: boolean | null): string {
  if (value === undefined || value === null) return '-'
  return value ? '是' : '否'
}
