import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  TransferOrderDTO,
  TransferOrderPageParam,
  TransferOrderPageVO,
  TransferOrderDetailVO,
  AvailableStockVO,
  AvailableStockPageParam,
  BatchStockQueryDTO,
  TransferSourceBatchVO,
  TransferTargetLocationVO
} from './types'

/**
 * 调拨单分页查询
 * @param pageParams 分页参数
 */
export function pageTransferOrder(pageParams: TransferOrderPageParam) {
  return httpClient.get<ApiResult<PageResult<TransferOrderPageVO>>>('/wms/transfer-order/page', {
    params: pageParams
  })
}

/**
 * 获取调拨单详情
 * @param id 调拨单ID
 */
export function getTransferOrderDetail(id: number) {
  return httpClient.get<ApiResult<TransferOrderDetailVO>>('/wms/transfer-order/detail', {
    params: { id }
  })
}

/**
 * 创建调拨单
 * @param dto 调拨单数据传输对象
 */
export function createTransferOrder(dto: TransferOrderDTO) {
  return httpClient.post<ApiResult<number>>('/wms/transfer-order', dto)
}

/**
 * 修改调拨单
 * @param dto 调拨单数据传输对象
 */
export function updateTransferOrder(dto: TransferOrderDTO) {
  return httpClient.put<ApiResult<void>>('/wms/transfer-order', dto)
}

/**
 * 确认出库
 * @param id 调拨单ID
 */
export function shipTransferOrder(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/transfer-order/ship', null, {
    params: { id }
  })
}

/**
 * 确认入库
 * @param id 调拨单ID
 */
export function receiveTransferOrder(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/transfer-order/receive', null, {
    params: { id }
  })
}

/**
 * 撤回调拨单
 * @param id 调拨单ID
 */
export function revokeTransferOrder(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/transfer-order/revoke', null, {
    params: { id }
  })
}

/**
 * 取消调拨单
 * @param id 调拨单ID
 */
export function cancelTransferOrder(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/transfer-order/cancel', null, {
    params: { id }
  })
}

/**
 * 删除调拨单
 * @param id 调拨单ID
 */
export function deleteTransferOrder(id: number) {
  return httpClient.delete<ApiResult<void>>('/wms/transfer-order', {
    params: { id }
  })
}

/**
 * 分页查询可调拨库存
 * @param params 分页参数
 */
export function pageAvailableStock(params: AvailableStockPageParam) {
  return httpClient.get<ApiResult<PageResult<AvailableStockVO>>>(
    '/wms/transfer-order/available-stock/page',
    {
      params
    }
  )
}

/**
 * 批量查询可用库存
 * @param dto 批量查询参数
 */
export function batchQueryStock(dto: BatchStockQueryDTO) {
  return httpClient.post<ApiResult<Record<string, number>>>(
    '/wms/transfer-order/available-stock/batch',
    dto
  )
}

/** 源批次候选（A仓·某货主可调拨批次） */
export function listSourceBatches(params: {
  fromWarehouseId: number
  erpTenantId: number
  skuKeyword?: string
}) {
  return httpClient.get<ApiResult<TransferSourceBatchVO[]>>('/wms/transfer-order/source-batches', {
    params
  })
}

/** 目标库位候选（B仓·该货主服务商租用标准库位） */
export function listTargetLocations(params: { toWarehouseId: number; erpTenantId: number }) {
  return httpClient.get<ApiResult<TransferTargetLocationVO[]>>(
    '/wms/transfer-order/target-locations',
    { params }
  )
}
