import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

export function submitOrderFulfillment(
  erpOrderId: number,
  wmsWarehouseId?: number,
  logisticsProductId?: number
) {
  return httpClient.post<ApiResult<number>>('/order/fulfillment/submit', {
    erpOrderId,
    wmsWarehouseId,
    logisticsProductId
  })
}

export function cancelOrderFulfillment(erpOrderId: number, reason?: string) {
  return httpClient.post<ApiResult<void>>(`/order/fulfillment/${erpOrderId}/cancel`, { reason })
}
