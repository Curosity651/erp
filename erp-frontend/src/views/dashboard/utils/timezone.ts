import dayjs, { Dayjs } from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

// 扩展 dayjs 插件
dayjs.extend(utc)
dayjs.extend(timezone)

// 莫斯科时区常量
const MOSCOW_TIMEZONE = 'Europe/Moscow'

/**
 * 获取莫斯科时区的当前时间
 * @returns 莫斯科时区的当前时间
 */
export function getMoscowNow(): Dayjs {
  return dayjs().tz(MOSCOW_TIMEZONE)
}

/**
 * 获取莫斯科时区的今天（00:00:00 - 23:59:59）
 * @returns { start: 'YYYY-MM-DD', end: 'YYYY-MM-DD' }
 */
export function getMoscowToday(): { start: string; end: string } {
  const now = getMoscowNow()
  return {
    start: now.format('YYYY-MM-DD'),
    end: now.format('YYYY-MM-DD')
  }
}

/**
 * 获取莫斯科时区的昨天
 * @returns { start: 'YYYY-MM-DD', end: 'YYYY-MM-DD' }
 */
export function getMoscowYesterday(): { start: string; end: string } {
  const yesterday = getMoscowNow().subtract(1, 'day')
  return {
    start: yesterday.format('YYYY-MM-DD'),
    end: yesterday.format('YYYY-MM-DD')
  }
}

/**
 * 获取莫斯科时区的最近N天
 * @param days 天数
 * @returns { start: 'YYYY-MM-DD', end: 'YYYY-MM-DD' }
 */
export function getMoscowLastNDays(days: number): { start: string; end: string } {
  const now = getMoscowNow()
  const start = now.subtract(days, 'day')
  return {
    start: start.format('YYYY-MM-DD'),
    end: now.format('YYYY-MM-DD')
  }
}

/**
 * 获取莫斯科时区的本月
 * @returns { start: 'YYYY-MM-DD', end: 'YYYY-MM-DD' }
 */
export function getMoscowThisMonth(): { start: string; end: string } {
  const now = getMoscowNow()
  return {
    start: now.startOf('month').format('YYYY-MM-DD'),
    end: now.endOf('month').format('YYYY-MM-DD')
  }
}

/**
 * 获取莫斯科时区的上月
 * @returns { start: 'YYYY-MM-DD', end: 'YYYY-MM-DD' }
 */
export function getMoscowLastMonth(): { start: string; end: string } {
  const now = getMoscowNow()
  const lastMonth = now.subtract(1, 'month')
  return {
    start: lastMonth.startOf('month').format('YYYY-MM-DD'),
    end: lastMonth.endOf('month').format('YYYY-MM-DD')
  }
}

/**
 * 格式化莫斯科时间为显示字符串
 * @param date 可选的日期对象或字符串，如果不传则使用当前时间
 * @returns 格式化后的莫斯科时间字符串 (YYYY-MM-DD HH:mm:ss MSK)
 */
export function formatMoscowTime(date?: Date | string): string {
  if (!date) {
    return getMoscowNow().format('YYYY-MM-DD HH:mm:ss') + ' MSK'
  }
  return dayjs(date).tz(MOSCOW_TIMEZONE).format('YYYY-MM-DD HH:mm:ss') + ' MSK'
}
