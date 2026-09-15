import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { ErpOrderPageParam, ErpOrderPageVO } from './types'

/**
 * 订单主表分页查询
 * @param pageParams 分页参数
 */
export function pageErpOrder(pageParams: ErpOrderPageParam) {
  return httpClient.get<ApiResult<ErpOrderPageVO>>('/order/wb-order/page', {
    params: pageParams
  })
}

/** 锁定 */
export function lockOrder(id: number) {
  return httpClient.patch<ApiResult<void>>(`/order/wb-order/${id}/lock`)
}

/** 解锁 */
export function unlockOrder(id: number) {
  return httpClient.patch<ApiResult<void>>(`/order/wb-order/${id}/unlock`)
}

/** 同步订单（支持单条/批量），仅 WB 平台有效；返回汇总 */
export function syncOrders(orderIds: number[]) {
  return httpClient.post<ApiResult<any>>('/order/wb-order/sync', orderIds)
}

/** 同步全部订单信息 */
export function syncAllOrders() {
  return httpClient.post<ApiResult<any>>('/order/wb-order/sync-all')
}

/**
 * 导出 Wildberries 订单
 * @param params 查询参数
 */
export function exportOrders(params: ErpOrderPageParam) {
  return httpClient.post('/order/wb-order/export', params, {
    responseType: 'blob'
  })
}
