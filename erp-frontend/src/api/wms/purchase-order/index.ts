import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  PurchaseOrderDTO,
  PurchaseOrderDetailVO,
  PurchaseOrderFileVO,
  PurchaseOrderPageParam,
  PurchaseOrderPageVO,
  PurchaseOrderQO
} from './types'

// ==================== 采购单基础 ====================

/**
 * 采购单分页查询
 * @param pageParams 分页参数
 */
export function pagePurchaseOrder(pageParams: PurchaseOrderPageParam) {
  return httpClient.get<ApiResult<PageResult<PurchaseOrderPageVO>>>('/wms/purchase-order/page', {
    params: pageParams
  })
}

/**
 * 获取采购单详情
 * @param id 采购单ID
 */
export function getPurchaseOrderDetail(id: number) {
  return httpClient.get<ApiResult<PurchaseOrderDetailVO>>('/wms/purchase-order/detail', {
    params: { id }
  })
}

/**
 * 创建采购单
 * @param dto 采购单数据
 */
export function createPurchaseOrder(dto: PurchaseOrderDTO) {
  return httpClient.post<ApiResult<number>>('/wms/purchase-order', dto)
}

/**
 * 编辑采购单
 * @param dto 采购单数据
 */
export function updatePurchaseOrder(dto: PurchaseOrderDTO) {
  return httpClient.put<ApiResult<void>>('/wms/purchase-order', dto)
}

/**
 * 删除采购单
 * @param id 采购单ID
 */
export function deletePurchaseOrder(id: number) {
  return httpClient.delete<ApiResult<void>>('/wms/purchase-order', {
    params: { id }
  })
}

// ==================== 状态管理 ====================

/**
 * 确认采购单
 * @param id 采购单ID
 */
export function confirmPurchaseOrder(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/purchase-order/confirm', null, {
    params: { id }
  })
}

/**
 * 开始生产
 * @param id 采购单ID
 */
export function startProduction(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/purchase-order/start-production', null, {
    params: { id }
  })
}

/**
 * 取消采购单
 * @param id 采购单ID
 */
export function cancelPurchaseOrder(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/purchase-order/cancel', null, {
    params: { id }
  })
}

// ==================== 导出 ====================

/**
 * 导出采购单
 * @param qo 查询条件
 */
export function exportPurchaseOrder(qo: PurchaseOrderQO) {
  return httpClient.get('/wms/purchase-order/export', {
    params: qo,
    responseType: 'blob'
  })
}
