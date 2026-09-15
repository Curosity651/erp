import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  OzonOrderPageParam,
  OzonOrderPageVO,
  SyncSummaryVO
} from './types'

/**
 * Ozon 订单分页查询
 * @param pageParams 分页参数
 */
export function pageOzonOrder(pageParams: OzonOrderPageParam) {
  return httpClient.get<ApiResult<OzonOrderPageVO>>('/order/ozon-order/page', {
    params: pageParams
  })
}

/**
 * 锁定订单
 * @param id 订单ID
 */
export function lockOzonOrder(id: number) {
  return httpClient.post<ApiResult<boolean>>(`/order/ozon-order/lock/${id}`)
}

/**
 * 解锁订单
 * @param id 订单ID
 */
export function unlockOzonOrder(id: number) {
  return httpClient.post<ApiResult<boolean>>(`/order/ozon-order/unlock/${id}`)
}

/**
 * 同步 Ozon 订单状态（支持单条/批量）
 * @param orderIds 订单ID列表
 */
export function syncOzonOrders(orderIds: number[]) {
  return httpClient.post<ApiResult<SyncSummaryVO>>('/order/ozon-order/sync', orderIds)
}

/**
 * 同步店铺全量订单
 * @param shopId 店铺ID
 */
export function syncOzonShop(shopId: number) {
  return httpClient.post<ApiResult<void>>(`/order/ozon-order/sync-shop/${shopId}`)
}

/**
 * 同步全量 Ozon 订单（所有启用店铺）
 */
export function syncAllOzonOrders() {
  return httpClient.post<ApiResult<void>>('/order/ozon-order/sync-all')
}

/**
 * 导出 Ozon 订单
 * @param params 查询参数
 */
export function exportOzonOrders(params: OzonOrderPageParam) {
  return httpClient.post('/order/ozon-order/export', params, {
    responseType: 'blob'
  })
}
