export interface WarehouseFulfillmentState {
  fulfillmentOrderId?: number
  warehouseFulfillmentStatus?: string
}

export function warehouseFulfillmentBlockReason(order: WarehouseFulfillmentState): string {
  const status = order.warehouseFulfillmentStatus?.trim()
  if (status === 'CANCELLED' || status === 'CANCEL_RETURNING') {
    return '仓库履约已取消，不允许再次提交'
  }
  if (status) return `仓库履约已提交（${status}）`
  if (order.fulfillmentOrderId) return '仓库履约已提交'
  return ''
}
