import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { YdOrderPageParam, YdOrderPageVO } from './types'

/**
 * Yandex 订单分页查询
 */
export function pageYdOrder(pageParams: YdOrderPageParam) {
  return httpClient.get<ApiResult<YdOrderPageVO>>('/order/yd-order/page', {
    params: pageParams
  })
}

/**
 * 锁定订单
 */
export function lockYdOrder(id: number) {
  return httpClient.post<ApiResult<boolean>>(`/order/yd-order/lock/${id}`)
}

/**
 * 解锁订单
 */
export function unlockYdOrder(id: number) {
  return httpClient.post<ApiResult<boolean>>(`/order/yd-order/unlock/${id}`)
}

/**
 * 按订单 ID 同步
 */
export function syncYdOrdersByIds(orderIds: number[]) {
  return httpClient.post<ApiResult<any>>('/order/yd-order/sync', orderIds)
}

/**
 * 全量同步
 */
export function syncAllYdOrders() {
  return httpClient.post<ApiResult<void>>('/order/yd-order/sync-all')
}
