import { describe, expect, it } from 'vitest'

import { menuLocaleKey, resolveMenuTitle } from './menu'

describe('menu localization', () => {
  it('uses a stable key derived from the complete route path', () => {
    expect(menuLocaleKey('/warehouse-mgmt/location-inventory')).toBe(
      'menu.warehouseMgmt.locationInventory'
    )
  })

  it('returns translated text and retains the backend title as fallback', () => {
    const translate = (key: string) =>
      key === 'menu.outboundOps.orderDispatch' ? 'Order dispatch' : key
    expect(resolveMenuTitle('/outbound-ops/order-dispatch', '订单下架', translate)).toBe(
      'Order dispatch'
    )
    expect(resolveMenuTitle('/new-page', '新页面', translate)).toBe('新页面')
  })
})
