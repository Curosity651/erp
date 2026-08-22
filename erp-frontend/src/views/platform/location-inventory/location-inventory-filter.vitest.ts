import { describe, expect, it } from 'vitest'
import type { LocationInventoryGrid } from '@/api/wms/location-inventory/types'
import { buildZoneOptions, filterLocationInventory } from './location-inventory-filter'

const row = (
  locationId: number,
  zoneId: number,
  zoneName: string,
  skuCodes: string[]
): LocationInventoryGrid => ({
  locationId,
  warehouseId: 53,
  locationCode: `A1-0${locationId}`,
  zoneId,
  zoneName,
  zoneType: zoneId === 1 ? 'STANDARD' : 'TEMP',
  skuCodes,
  capacityVolumeMm3: 1,
  usedVolumeMm3: 0,
  maxWeightGrams: 1,
  usedWeightGrams: 0,
  utilizationPercent: 0,
  skuKindCount: skuCodes.length,
  totalQuantity: 0,
  reservedQuantity: 0,
  availableQuantity: 0,
  volumeExceeded: false,
  weightExceeded: false,
  capacityDataComplete: true
})

describe('location inventory filters', () => {
  const rows = [row(1, 1, '标准区', ['ABC-001']), row(2, 2, '暂存区', ['XYZ-002'])]

  it('filters by SKU without case sensitivity', () => {
    expect(filterLocationInventory(rows, 'abc', undefined).map(item => item.locationId)).toEqual([1])
  })

  it('combines SKU and zone filters', () => {
    expect(filterLocationInventory(rows, '002', 2).map(item => item.locationId)).toEqual([2])
    expect(filterLocationInventory(rows, '001', 2)).toEqual([])
  })

  it('builds distinct zone options in source order', () => {
    expect(buildZoneOptions([...rows, row(3, 1, '标准区', [])])).toEqual([
      { value: 1, label: '标准区' },
      { value: 2, label: '暂存区' }
    ])
  })
})
