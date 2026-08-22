import { describe, expect, it } from 'vitest'
import { canOpenNextOrder, canPrintLabel, failedOrderIds } from './workbench-flow'

describe('fulfillment workbench flow', () => {
  it('prints only after picking and unlocks the next order only after packing', () => {
    expect(canPrintLabel('PICKING')).toBe(false)
    expect(canPrintLabel('WAITING_PACK')).toBe(true)
    expect(canOpenNextOrder('WAITING_PACK')).toBe(false)
    expect(canOpenNextOrder('COMPLETED')).toBe(true)
  })

  it('keeps only failed orders selected after batch signout', () => {
    expect(failedOrderIds({ 12: '缺少费率', 15: '库存已变化' })).toEqual([12, 15])
  })
})
