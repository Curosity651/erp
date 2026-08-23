import { describe, expect, it } from 'vitest'
import * as flow from './workbench-flow'

const { failedOrderIds } = flow

describe('fulfillment workbench flow', () => {
  it('keeps only failed orders selected after batch signout', () => {
    expect(failedOrderIds({ 12: '缺少费率', 15: '库存已变化' })).toEqual([12, 15])
  })

  it('allows selecting only orders waiting to be shelved', () => {
    expect(flow.isShelfOrderSelectable).toBeTypeOf('function')
    expect(flow.isShelfOrderSelectable?.('WAITING_SHELF')).toBe(true)
    expect(flow.isShelfOrderSelectable?.('WAITING_PICK')).toBe(false)
    expect(flow.isShelfOrderSelectable?.('PICKING')).toBe(false)
  })
})
