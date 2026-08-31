import { describe, expect, it } from 'vitest'
import { isPricingLocked, suggestVersionCode } from './product-lifecycle'

describe('logistics product lifecycle', () => {
  it('locks pricing identity after a product has business orders', () => {
    expect(isPricingLocked({ used: true, orderReferenceCount: 1 })).toBe(true)
    expect(isPricingLocked({ used: false, orderReferenceCount: 0 })).toBe(false)
  })

  it('suggests the next product version code when copying for repricing', () => {
    expect(suggestVersionCode('STANDARD')).toBe('STANDARD-V2')
    expect(suggestVersionCode('STANDARD-V2')).toBe('STANDARD-V3')
    expect(suggestVersionCode(' small-eco-v9 ')).toBe('SMALL-ECO-V10')
  })
})
