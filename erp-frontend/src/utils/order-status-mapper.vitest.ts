import { describe, expect, it } from 'vitest'
import { OWNER_ORDER_STATUS_MAP } from '../api/order/types'

describe('owner order business status', () => {
  it.each([
    ['PENDING_CONFIRM', '待确认'],
    ['WAITING_SHELF', '待下架'],
    ['OUTBOUND_PROCESSING', '出库作业中'],
    ['HANDED_OVER', '已交接'],
    ['CANCELLED', '已取消'],
    ['EXCEPTION', '异常']
  ])('maps %s to %s', (status, label) => {
    expect(OWNER_ORDER_STATUS_MAP[status as keyof typeof OWNER_ORDER_STATUS_MAP].label).toBe(label)
  })
})
