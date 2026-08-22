import { describe, expect, it } from 'vitest'
import {
  allocatedQuantity,
  capacityState,
  createInitialRow,
  mergeRecordLines,
  requiresCapacityReason
} from './putaway-record'

describe('putaway actual-result helpers', () => {
  it('starts with an empty location and quantity', () => {
    expect(createInitialRow('SKU-A')).toMatchObject({
      skuCode: 'SKU-A',
      quality: 'GOOD',
      locationId: undefined,
      quantity: undefined
    })
  })

  it('merges duplicate location sku and quality lines', () => {
    const lines = mergeRecordLines([
      { skuCode: 'SKU-A', quality: 'GOOD', locationId: 1, quantity: 3 },
      {
        skuCode: 'SKU-A',
        quality: 'GOOD',
        locationId: 1,
        quantity: 2,
        capacityOverrideReason: '现场已放置'
      }
    ])
    expect(lines).toHaveLength(1)
    expect(lines[0]).toMatchObject({ quantity: 5, capacityOverrideReason: '现场已放置' })
    expect(allocatedQuantity(lines, 'SKU-A')).toBe(5)
  })

  it('classifies utilization from normal through overflow', () => {
    expect(capacityState({ calculable: true, utilizationPercent: 79 })).toBe('normal')
    expect(capacityState({ calculable: true, utilizationPercent: 80 })).toBe('warning')
    expect(capacityState({ calculable: true, utilizationPercent: 100 })).toBe('warning')
    expect(capacityState({ calculable: true, utilizationPercent: 101 })).toBe('overflow')
    expect(capacityState({ calculable: false })).toBe('unknown')
  })

  it('requires a reason only for calculable volume or weight overflow', () => {
    expect(requiresCapacityReason({ calculable: true, volumeAllowed: false, weightAllowed: true })).toBe(true)
    expect(requiresCapacityReason({ calculable: true, volumeAllowed: true, weightAllowed: false })).toBe(true)
    expect(requiresCapacityReason({ calculable: true, volumeAllowed: true, weightAllowed: true, skuKindsAllowed: false })).toBe(false)
    expect(requiresCapacityReason({ calculable: false })).toBe(false)
  })
})
