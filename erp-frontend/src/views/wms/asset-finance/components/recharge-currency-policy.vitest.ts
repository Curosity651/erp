import { describe, expect, it } from 'vitest'
import { defaultRechargeCurrency, rechargeCurrencyOptions } from './recharge-currency-policy'

describe('recharge currency policy', () => {
  it('uses the platform billing currency for WMS-to-platform recharge', () => {
    expect(defaultRechargeCurrency('platform')).toBe('CNY')
    expect(rechargeCurrencyOptions('platform')).toEqual([{ label: 'CNY', value: 'CNY' }])
  })

  it('keeps multi-currency selection for owner-to-WMS recharge', () => {
    expect(defaultRechargeCurrency('owner')).toBe('RUB')
    expect(rechargeCurrencyOptions('owner').map(item => item.value)).toContain('CNY')
  })
})
