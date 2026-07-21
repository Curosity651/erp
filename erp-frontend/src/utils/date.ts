import dayjs from 'dayjs'

/**
 * 格式化日期为 MM-DD 格式
 * @example formatDateShort('2026-02-03') => '02-03'
 */
export function formatDateShort(dateStr: string): string {
  return dayjs(dateStr).format('MM-DD')
}

/**
 * 格式化日期为 M/D 格式
 * @example formatDateSlash('2026-02-03') => '2/3'
 */
export function formatDateSlash(dateStr: string): string {
  return dayjs(dateStr).format('M/D')
}

/**
 * 格式化日期为中文格式
 * @example formatDateChinese('2026-02-03') => '2月3日'
 */
export function formatDateChinese(dateStr: string): string {
  return dayjs(dateStr).format('M月D日')
}

/**
 * 格式化日期为中文格式（含今天标识）
 * @example formatDateChineseWithToday('2026-02-03') => '今天 2月3日' 或 '2月4日'
 */
export function formatDateChineseWithToday(dateStr: string): string {
  const date = dayjs(dateStr)
  const isToday = date.isSame(dayjs(), 'day')
  const formatted = date.format('M月D日')
  return isToday ? `今天 ${formatted}` : formatted
}
