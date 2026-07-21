import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  ShippingOrderDTO,
  ShippingOrderPageParam,
  ShippingOrderPageVO,
  ShippingOrderDetailVO,
  ShippingOrderSimpleVO,
  AvailableItemQO,
  AvailableItemVO,
  RelatedInboundVO
} from './types'

/**
 * 物流单分页查询
 * @param pageParams 分页参数
 */
export function pageShippingOrder(pageParams: ShippingOrderPageParam) {
  return httpClient.get<ApiResult<PageResult<ShippingOrderPageVO>>>('/wms/shipping-order/page', {
    params: pageParams
  })
}

/**
 * 获取物流单详情
 * @param id 物流单ID
 */
export function getShippingOrderDetail(id: number) {
  return httpClient.get<ApiResult<ShippingOrderDetailVO>>('/wms/shipping-order/detail', {
    params: { id }
  })
}

/**
 * 创建物流单
 * @param dto 物流单数据传输对象
 */
export function createShippingOrder(dto: ShippingOrderDTO) {
  return httpClient.post<ApiResult<number>>('/wms/shipping-order', dto)
}

/**
 * 修改物流单
 * @param dto 物流单数据传输对象
 */
export function updateShippingOrder(dto: ShippingOrderDTO) {
  return httpClient.put<ApiResult<void>>('/wms/shipping-order', dto)
}

/**
 * 确认发货
 * @param id 物流单ID
 */
export function confirmShip(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/shipping-order/ship', null, {
    params: { id }
  })
}

/**
 * 删除物流单
 * @param ids 物流单ID列表
 */
export function deleteShippingOrder(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/shipping-order', {
    data: ids
  })
}

/**
 * 更新付款状态
 * @param id 物流单ID
 * @param paymentStatus 付款状态: 0-未付 / 1-已付
 * @param voucherFileId 付款凭证文件ID（paymentStatus=1时必传）
 */
export function updatePaymentStatus(id: number, paymentStatus: number, voucherFileId?: number) {
  return httpClient.patch<ApiResult<void>>('/wms/shipping-order/payment', null, {
    params: { id, paymentStatus, voucherFileId }
  })
}

/**
 * 获取可发货采购明细
 * @param qo 查询条件
 */
export function getAvailableItems(qo: AvailableItemQO) {
  return httpClient.get<ApiResult<AvailableItemVO[]>>('/wms/shipping-order/available-items', {
    params: qo
  })
}

/**
 * 导出物流单
 * @param qo 查询条件
 */
export function exportShippingOrder(qo: ShippingOrderPageParam) {
  return httpClient.get('/wms/shipping-order/export', {
    params: qo,
    responseType: 'blob'
  })
}

/**
 * 根据采购单ID查询关联物流单
 * @param purchaseOrderId 采购单ID
 */
export function getShippingOrdersByPurchaseOrderId(purchaseOrderId: number) {
  return httpClient.get<ApiResult<ShippingOrderSimpleVO[]>>(
    '/wms/shipping-order/by-purchase-order',
    {
      params: { purchaseOrderId }
    }
  )
}

/**
 * 查询关联入库单
 * @param shippingOrderId 物流单ID
 */
export function getRelatedInbounds(shippingOrderId: number) {
  return httpClient.get<ApiResult<RelatedInboundVO[]>>('/wms/shipping-order/related-inbounds', {
    params: { shippingOrderId }
  })
}
