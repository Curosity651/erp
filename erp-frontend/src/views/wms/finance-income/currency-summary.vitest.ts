import { describe, expect, it } from 'vitest'
import { formatCurrency } from './currency'

describe('provider finance currency formatting', () => {
  it('formats each amount with its own currency', () => {
    expect(formatCurrency(120, 'RUB')).toContain('₽')
    expect(formatCurrency(120, 'CNY')).toMatch(/¥|CN¥/)
    expect(formatCurrency(120, 'USD')).toContain('$')
  })

  it('does not silently label an unknown currency as rubles', () => {
    expect(formatCurrency(120, 'UNKNOWN')).toBe('120.00 UNKNOWN')
  })
})
