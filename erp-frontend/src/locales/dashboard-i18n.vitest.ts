import { describe, expect, it } from 'vitest'
import zhDashboard from './lang/zh-CN/dashboard.json'
import enDashboard from './lang/en-US/dashboard.json'
import ukDashboard from './lang/uk-UA/dashboard.json'
import ruDashboard from './lang/ru-RU/dashboard.json'
import zhMenu from './lang/zh-CN/menu.json'
import enMenu from './lang/en-US/menu.json'
import ukMenu from './lang/uk-UA/menu.json'
import ruMenu from './lang/ru-RU/menu.json'

describe('ERP dashboard internationalization', () => {
  it('keeps dashboard keys aligned across all supported languages', () => {
    const keySets = [zhDashboard, enDashboard, ukDashboard, ruDashboard].map(locale =>
      Object.keys(locale).sort()
    )
    expect(keySets[1]).toEqual(keySets[0])
    expect(keySets[2]).toEqual(keySets[0])
    expect(keySets[3]).toEqual(keySets[0])
  })

  it('provides localized ERP dashboard and sales target menu titles', () => {
    for (const locale of [zhMenu, enMenu, ukMenu, ruMenu]) {
      expect(locale.menu.statistics.dashboard).toBeTruthy()
      expect(locale.menu.statistics.salesTarget).toBeTruthy()
    }
  })
})
