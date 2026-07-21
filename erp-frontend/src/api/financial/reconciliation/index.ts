import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type {
  OrderReconciliationDetailVO,
  OrderReconciliationQO,
  OrderReconciliationStatsVO,
  OrderReconciliationVO
} from './types'

// ===================== 订单对账相关 =====================

/**
 * 获取订单对账统计数据
 * @param qo 查询参数
 */
export function getOrderReconciliationStats(qo: OrderReconciliationQO) {
  return httpClient.post<ApiResult<OrderReconciliationStatsVO>>(
    '/financial/reconciliation/orders/stats',
    qo
  )
}

/**
 * 分页查询订单对账列表
 * @param pageParam 分页参数
 * @param qo 查询参数
 */
export function pageOrderReconciliation(pageParam: PageParam, qo: OrderReconciliationQO) {
  return httpClient.post<ApiResult<PageResult<OrderReconciliationVO>>>(
    '/financial/reconciliation/orders',
    qo,
    {
      params: pageParam
    }
  )
}

/**
 * 获取订单对账详情
 * @param orderId 订单ID
 * @param periodType 周期类型
 */
export function getOrderReconciliationDetail(orderId: number, periodType: string) {
  return httpClient.get<ApiResult<OrderReconciliationDetailVO>>(
    '/financial/reconciliation/orders/detail',
    { params: { orderId, periodType } }
  )
}

/**
 * 导出订单对账数据
 * @param qo 查询参数
 */
export function exportOrderReconciliation(qo: OrderReconciliationQO) {
  return httpClient.post('/financial/reconciliation/orders/export', qo, {
    responseType: 'blob'
  })
}
