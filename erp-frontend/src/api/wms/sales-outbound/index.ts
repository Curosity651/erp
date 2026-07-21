import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  SalesOutboundDTO,
  SalesOutboundPageParam,
  SalesOutboundPageVO,
  SalesOutboundDetailVO,
  SalesOutboundQO,
  PendingOrderVO,
  PendingOrderPageParam,
  StockShortageVO
} from './types'

/**
 * 销售出库单分页查询
 * @param pageParams 分页参数
 */
export function pageSalesOutbound(pageParams: SalesOutboundPageParam) {
  return httpClient.get<ApiResult<PageResult<SalesOutboundPageVO>>>('/wms/sales-outbound/page', {
    params: pageParams
  })
}

/**
 * 获取销售出库单详情
 * @param id 出库单ID
 */
export function getSalesOutboundDetail(id: number) {
  return httpClient.get<ApiResult<SalesOutboundDetailVO>>('/wms/sales-outbound/detail', {
    params: { id }
  })
}

/**
 * 创建销售出库单
 * @param dto 出库单数据传输对象
 */
export function createSalesOutbound(dto: SalesOutboundDTO) {
  return httpClient.post<ApiResult<number>>('/wms/sales-outbound', dto)
}

/**
 * 修改销售出库单
 * @param dto 出库单数据传输对象
 */
export function updateSalesOutbound(dto: SalesOutboundDTO) {
  return httpClient.put<ApiResult<void>>('/wms/sales-outbound', dto)
}

/**
 * 确认出库
 * @param id 出库单ID
 * @returns 库存不足时返回不足明细列表
 */
export function confirmOutbound(id: number) {
  return httpClient.patch<ApiResult<StockShortageVO[]>>('/wms/sales-outbound/confirm', null, {
    params: { id }
  })
}

/**
 * 取消出库单
 * @param id 出库单ID
 */
export function cancelOutbound(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/sales-outbound/cancel', null, {
    params: { id }
  })
}

/**
 * 删除出库单（仅草稿）
 * @param ids 出库单ID列表
 */
export function deleteSalesOutbound(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/sales-outbound', {
    data: ids
  })
}

/**
 * 获取待出库订单列表（分页）
 * @param params 分页查询参数
 */
export function getPendingOrders(params: PendingOrderPageParam) {
  return httpClient.get<ApiResult<PageResult<PendingOrderVO>>>('/wms/sales-outbound/pending-orders', {
    params
  })
}

/**
 * 导出销售出库单
 * @param qo 查询条件
 */
export function exportSalesOutbound(qo: SalesOutboundQO) {
  return httpClient.get('/wms/sales-outbound/export', {
    params: qo,
    responseType: 'blob'
  })
}
