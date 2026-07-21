import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  PurchaseInboundDTO,
  PurchaseInboundPageParam,
  PurchaseInboundPageVO,
  PurchaseInboundDetailVO,
  AvailableShippingQO,
  AvailableShippingVO,
  ShippingItemForInboundVO
} from './types'

/**
 * 采购入库单分页查询
 * @param pageParams 分页参数
 */
export function pagePurchaseInbound(pageParams: PurchaseInboundPageParam) {
  return httpClient.get<ApiResult<PageResult<PurchaseInboundPageVO>>>(
    '/wms/purchase-inbound/page',
    {
      params: pageParams
    }
  )
}

/**
 * 获取采购入库单详情
 * @param id 入库单ID
 */
export function getPurchaseInboundDetail(id: number) {
  return httpClient.get<ApiResult<PurchaseInboundDetailVO>>('/wms/purchase-inbound/detail', {
    params: { id }
  })
}

/**
 * 创建采购入库单
 * @param dto 入库单数据传输对象
 */
export function createPurchaseInbound(dto: PurchaseInboundDTO) {
  return httpClient.post<ApiResult<number>>('/wms/purchase-inbound', dto)
}

/**
 * 修改采购入库单
 * @param dto 入库单数据传输对象
 */
export function updatePurchaseInbound(dto: PurchaseInboundDTO) {
  return httpClient.put<ApiResult<void>>('/wms/purchase-inbound', dto)
}

/**
 * 提交入库单（草稿 → 已提交），提交后流转给平台收货/上架
 * @param id 入库单ID
 */
export function submitInbound(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/purchase-inbound/submit', null, {
    params: { id }
  })
}

/**
 * 取消入库单
 * @param id 入库单ID
 */
export function cancelInbound(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/purchase-inbound/cancel', null, {
    params: { id }
  })
}

/**
 * 删除采购入库单
 * @param ids 入库单ID列表
 */
export function deletePurchaseInbound(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/purchase-inbound', {
    data: ids
  })
}

/**
 * 获取可入库物流单列表
 * @param qo 查询条件
 */
export function getAvailableShipping(qo: AvailableShippingQO) {
  return httpClient.get<ApiResult<AvailableShippingVO[]>>(
    '/wms/purchase-inbound/available-shipping',
    {
      params: qo
    }
  )
}

/**
 * 获取物流单待入库明细
 * @param shippingOrderId 物流单ID
 */
export function getShippingItemsForInbound(shippingOrderId: number) {
  return httpClient.get<ApiResult<ShippingItemForInboundVO[]>>(
    '/wms/purchase-inbound/shipping-items',
    {
      params: { shippingOrderId }
    }
  )
}

/**
 * 获取指定物流单信息（用于入库单创建页回填）
 * @param shippingOrderId 物流单ID
 */
export function getShippingDetail(shippingOrderId: number) {
  return httpClient.get<ApiResult<AvailableShippingVO>>('/wms/purchase-inbound/shipping-detail', {
    params: { shippingOrderId }
  })
}

/**
 * 导出采购入库单
 * @param qo 查询条件
 */
export function exportPurchaseInbound(qo: PurchaseInboundPageParam) {
  return httpClient.get('/wms/purchase-inbound/export', {
    params: qo,
    responseType: 'blob'
  })
}
