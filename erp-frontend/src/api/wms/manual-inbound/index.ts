import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type { ManualInboundDTO, ManualInboundPageParam } from './types'
import type {
  PurchaseInboundPageVO,
  PurchaseInboundDetailVO
} from '@/api/wms/purchase-inbound/types'

/**
 * 自定义入库单分页查询
 * @param pageParams 分页参数
 */
export function pageManualInbound(pageParams: ManualInboundPageParam) {
  return httpClient.get<ApiResult<PageResult<PurchaseInboundPageVO>>>('/wms/manual-inbound/page', {
    params: pageParams
  })
}

/**
 * 获取自定义入库单详情
 * @param id 入库单ID
 */
export function getManualInboundDetail(id: number) {
  return httpClient.get<ApiResult<PurchaseInboundDetailVO>>('/wms/manual-inbound/detail', {
    params: { id }
  })
}

/**
 * 创建自定义入库单
 * @param dto 入库单数据传输对象
 */
export function createManualInbound(dto: ManualInboundDTO) {
  return httpClient.post<ApiResult<number>>('/wms/manual-inbound', dto)
}

/**
 * 修改自定义入库单
 * @param dto 入库单数据传输对象
 */
export function updateManualInbound(dto: ManualInboundDTO) {
  return httpClient.put<ApiResult<void>>('/wms/manual-inbound', dto)
}

/**
 * 提交入库单（草稿 → 已提交），提交后流转给平台收货/上架
 * @param id 入库单ID
 */
export function submitManualInbound(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/manual-inbound/submit', null, {
    params: { id }
  })
}

/**
 * 取消入库单
 * @param id 入库单ID
 */
export function cancelManualInbound(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/manual-inbound/cancel', null, {
    params: { id }
  })
}

/**
 * 删除自定义入库单
 * @param ids 入库单ID列表
 */
export function deleteManualInbound(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/manual-inbound', {
    data: ids
  })
}

/**
 * 导出自定义入库单
 * @param qo 查询条件
 */
export function exportManualInbound(qo: ManualInboundPageParam) {
  return httpClient.get('/wms/manual-inbound/export', {
    params: qo,
    responseType: 'blob'
  })
}
