import { describe, expect, it } from 'vitest'

import { resolveInitialLocale } from './locale-contract'

describe('initial locale resolution', () => {
  it('restores a supported stored language', () => {
    expect(resolveInitialLocale('uk-UA')).toBe('uk-UA')
  })

  it('falls back to Chinese for missing or invalid values', () => {
    expect(resolveInitialLocale()).toBe('zh-CN')
    expect(resolveInitialLocale('de-DE')).toBe('zh-CN')
  })
})
