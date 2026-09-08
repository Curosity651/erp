import { describe, expect, it } from 'vitest'

import { formatLocaleDate, formatLocaleNumber } from './locale-format'

describe('locale formatting', () => {
  it('formats numbers using the selected locale', () => {
    expect(formatLocaleNumber(1234.5, 'en-US')).toBe('1,234.5')
    expect(formatLocaleNumber(1234.5, 'ru-RU')).toContain('1')
    expect(formatLocaleNumber(1234.5, 'ru-RU')).toContain('234,5')
  })

  it('formats dates using the selected locale', () => {
    const date = new Date(2026, 8, 8)
    expect(formatLocaleDate(date, 'zh-CN')).toContain('2026')
    expect(formatLocaleDate(date, 'uk-UA')).toContain('2026')
  })
})
