import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  ReturnInboundDTO,
  ReturnInboundPageParam,
  ReturnInboundPageVO,
  ReturnInboundDetailVO,
  ReturnableOrderPageParam,
  ReturnableOrderVO,
  OrderReturnableItemVO
} from './types'

/**
 * 退货入库单分页查询
 * @param pageParams 分页参数
 */
export function pageReturnInbound(pageParams: ReturnInboundPageParam) {
  return httpClient.get<ApiResult<PageResult<ReturnInboundPageVO>>>('/wms/return-inbound/page', {
    params: pageParams
  })
}

/**
 * 获取退货入库单详情
 * @param id 退货单ID
 */
export function getReturnInboundDetail(id: number) {
  return httpClient.get<ApiResult<ReturnInboundDetailVO>>('/wms/return-inbound/detail', {
    params: { id }
  })
}

/**
 * 创建退货入库单（一步到位，包含质检信息）
 * @param dto 退货单数据传输对象
 */
export function createReturnInbound(dto: ReturnInboundDTO) {
  return httpClient.post<ApiResult<number>>('/wms/return-inbound', dto)
}

/**
 * 获取可退货订单列表
 * @param pageParams 分页参数和筛选条件
 */
export function getReturnableOrders(pageParams: ReturnableOrderPageParam) {
  return httpClient.get<ApiResult<PageResult<ReturnableOrderVO>>>(
    '/wms/return-inbound/returnable-orders',
    {
      params: pageParams
    }
  )
}

/**
 * 获取订单可退货明细
 * @param orderId 订单ID
 */
export function getOrderItems(orderId: number) {
  return httpClient.get<ApiResult<OrderReturnableItemVO[]>>('/wms/return-inbound/order-items', {
    params: { orderId }
  })
}

/**
 * 导出退货入库单
 * @param qo 查询条件
 */
export function exportReturnInbound(qo: ReturnInboundPageParam) {
  return httpClient.get('/wms/return-inbound/export', {
    params: qo,
    responseType: 'blob'
  })
}
