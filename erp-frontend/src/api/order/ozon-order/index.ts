import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  OzonOrderPageParam,
  OzonOrderPageVO,
  LabelBatchVO,
  SyncSummaryVO,
  OzonActBatchVO,
  OzonPickListBatchVO,
  OzonDeliveryMethodRule
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
 * 批量确认 Ozon 订单（仅 FBS 订单）
 * @param orderIds 订单ID列表
 */
export function confirmOzonOrders(orderIds: number[]) {
  return httpClient.post<ApiResult<any>>('/order/ozon-order/confirm', orderIds)
}

/**
 * 批量打印 Ozon 面单（仅 FBS 订单）
 * @param orderIds 订单ID列表
 */
export function printOzonLabels(orderIds: number[]) {
  return httpClient.post<ApiResult<LabelBatchVO>>('/order/ozon-order/print-labels', orderIds)
}

/**
 * 打印拣货单（按店铺分组，每个店铺一份 PDF）
 * @param orderIds 订单ID列表
 */
export function printOzonPickList(orderIds: number[]) {
  return httpClient.post<ApiResult<OzonPickListBatchVO>>('/order/ozon-order/pick-list', orderIds)
}

/**
 * 准备发运：生成运单(act)。立即返回 batchNo，运单在 Ozon 侧异步生成，
 * 需配合 getOzonActBatch 轮询直至 READY / FAILED。
 * @param orderIds      订单ID列表（仅大仓 FBS 订单会被纳入）
 * @param departureDate 发货日期 yyyy-MM-dd
 */
export function createOzonActs(orderIds: number[], departureDate: string) {
  return httpClient.post<ApiResult<OzonActBatchVO>>('/order/ozon-order/act/create', {
    orderIds,
    departureDate
  })
}

/**
 * 查询运单批次状态（轮询用）。后端每次调用只对 PENDING 的运单查一次 Ozon 状态，
 * 就绪则立即拉取 PDF 并落 OSS。
 * @param batchNo 批次号
 */
export function getOzonActBatch(batchNo: string) {
  return httpClient.get<ApiResult<OzonActBatchVO>>('/order/ozon-order/act/batch', {
    params: { batchNo }
  })
}

export function listOzonDeliveryMethodRules() {
  return httpClient.get<ApiResult<OzonDeliveryMethodRule[]>>('/order/ozon-order/delivery-method-rules')
}

export function saveOzonDeliveryMethodRule(rule: OzonDeliveryMethodRule) {
  return httpClient.post<ApiResult<number>>('/order/ozon-order/delivery-method-rules', rule)
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
