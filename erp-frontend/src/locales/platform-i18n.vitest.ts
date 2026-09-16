import { describe, expect, it } from 'vitest'

import zhPlatform from './lang/zh-CN/platform.json'
import enPlatform from './lang/en-US/platform.json'
import ukPlatform from './lang/uk-UA/platform.json'
import ruPlatform from './lang/ru-RU/platform.json'

const resources = [zhPlatform, enPlatform, ukPlatform, ruPlatform]

describe('overseas warehouse platform internationalization', () => {
  it('keeps platform keys aligned across all supported languages', () => {
    const keySets = resources.map(locale => Object.keys(locale).sort())
    expect(keySets[1]).toEqual(keySets[0])
    expect(keySets[2]).toEqual(keySets[0])
    expect(keySets[3]).toEqual(keySets[0])
  })

  it('contains translated resources for the core operator workflows', () => {
    const requiredKeys = [
      'platform.operator.title',
      'platform.return.title',
      'platform.return.process.title',
      'platform.return.receipt.globalSku',
      'platform.return.receipt.downloadTemplate',
      'platform.return.receipt.importExcel',
      'platform.return.receipt.resolving',
      'platform.return.receipt.unmatched',
      'platform.return.receipt.importSuccess',
      'platform.return.receipt.importFailed',
      'platform.return.receipt.importEmpty',
      'platform.return.receipt.maxRows',
      'platform.return.process.globalSkuMissing',
      'platform.picking.title',
      'platform.picking.simple.instructions',
      'platform.picking.package.export',
      'platform.dashboard.filter.timeRange',
      'platform.dashboard.kpi.onHand',
      'platform.dashboard.zone.inventoryUnits',
      'platform.dashboard.throughput.inbound'
    ]

    for (const locale of resources) {
      for (const key of requiredKeys) expect(locale[key as keyof typeof locale]).toBeTruthy()
    }
    for (const key of requiredKeys) {
      expect(new Set(resources.map(locale => locale[key as keyof typeof locale])).size).toBe(4)
    }
  })

  it('keeps interpolation variables consistent across locales', () => {
    const variables = (value: string) =>
      [...value.matchAll(/\{([^}]+)\}/g)].map(match => match[1]).sort()
    for (const key of Object.keys(zhPlatform)) {
      const expected = variables(zhPlatform[key as keyof typeof zhPlatform])
      for (const locale of resources.slice(1)) {
        expect(variables(locale[key as keyof typeof locale]), key).toEqual(expected)
      }
    }
  })
})
