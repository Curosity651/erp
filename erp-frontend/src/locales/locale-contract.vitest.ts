import { describe, expect, it } from 'vitest'

import { DEFAULT_LOCALE, SUPPORTED_LOCALES, isSupportedLocale } from './locale-contract'

describe('locale contract', () => {
  it('supports exactly the approved locales', () => {
    expect(SUPPORTED_LOCALES).toEqual(['zh-CN', 'en-US', 'uk-UA', 'ru-RU'])
    expect(DEFAULT_LOCALE).toBe('zh-CN')
    expect(isSupportedLocale('ru-RU')).toBe(true)
    expect(isSupportedLocale('de-DE')).toBe(false)
  })
})
