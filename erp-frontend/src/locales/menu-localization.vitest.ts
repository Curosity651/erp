import { describe, expect, it } from 'vitest'

import { menuLocaleKey, resolveMenuLocaleKey, resolveMenuTitle } from './menu'
import enMenu from './lang/en-US/menu.json'

const hasEnglishMenuKey = (key: string) => {
  let value: unknown = enMenu
  for (const segment of key.split('.')) {
    if (!value || typeof value !== 'object' || !(segment in value)) return false
    value = (value as Record<string, unknown>)[segment]
  }
  return typeof value === 'string'
}

describe('menu localization', () => {
  it('uses a stable key derived from the complete route path', () => {
    expect(menuLocaleKey('/warehouse-mgmt/location-inventory')).toBe(
      'menu.warehouseMgmt.locationInventory'
    )
  })

  it('returns translated text and retains the backend title as fallback', () => {
    const translate = (key: string) =>
      key === 'menu.outboundOps.orderDispatch' ? 'Order dispatch' : key
    const exists = (key: string) => key === 'menu.outboundOps.orderDispatch'
    expect(resolveMenuTitle('/outbound-ops/order-dispatch', '订单下架', translate, exists)).toBe(
      'Order dispatch'
    )
    expect(resolveMenuTitle('/new-page', '新页面', translate, exists)).toBe('新页面')
  })

  it('uses a dedicated title key when a parent menu also contains children', () => {
    const exists = (key: string) =>
      ['menu.statistics', 'menu.statistics._title', 'menu.statistics.dashboard'].includes(key)
    expect(resolveMenuLocaleKey('/statistics', exists)).toBe('menu.statistics._title')
    expect(resolveMenuLocaleKey('/statistics/dashboard', exists)).toBe('menu.statistics.dashboard')
    expect(
      resolveMenuTitle('/statistics', '数据分析', key =>
        key === 'menu.statistics._title' ? 'Data analytics' : key, exists)
    ).toBe('Data analytics')
  })

  it('covers every menu route visible on the overseas warehouse platform', () => {
    const paths = [
      '/log',
      '/customer-mgmt',
      '/warehouse-mgmt',
      '/ops',
      '/ops/inbound-ops',
      '/ops/putaway',
      '/ops/fulfillment-shelf',
      '/ops/fulfillment-picking',
      '/ops/fulfillment-workbench',
      '/ops/return-qc',
      '/ops/stocktake',
      '/ops/location-transfer',
      '/platform-finance',
      '/statistics/platform-dashboard',
      '/system',
      '/system/role',
      '/system/user',
      '/system/organization',
      '/system/project-group',
      '/system/position',
      '/system/shop',
      '/system/menu',
      '/system/dict',
      '/system/config'
    ]

    for (const path of paths) {
      expect(resolveMenuLocaleKey(path, hasEnglishMenuKey), path).toBeTruthy()
    }
  })
})
