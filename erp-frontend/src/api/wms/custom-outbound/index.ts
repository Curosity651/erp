import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  BatchStockQueryDTO,
  CustomOutboundDTO,
  CustomOutboundDetailVO,
  CustomOutboundPageParam,
  CustomOutboundPageVO,
  CustomOutboundQO,
  StockShortageVO
} from './types'

/**
 * 自定义出库单分页查询
 * @param pageParams 分页参数
 */
export function pageCustomOutbound(pageParams: CustomOutboundPageParam) {
  return httpClient.get<ApiResult<PageResult<CustomOutboundPageVO>>>('/wms/custom-outbound/page', {
    params: pageParams
  })
}

/**
 * 获取自定义出库单详情
 * @param id 出库单ID
 */
export function getCustomOutboundDetail(id: number) {
  return httpClient.get<ApiResult<CustomOutboundDetailVO>>('/wms/custom-outbound/detail', {
    params: { id }
  })
}

/**
 * 创建自定义出库单（草稿）
 * @param dto 出库单数据传输对象
 */
export function createCustomOutbound(dto: CustomOutboundDTO) {
  return httpClient.post<ApiResult<number>>('/wms/custom-outbound', dto)
}

/**
 * 修改自定义出库单（仅草稿）
 * @param dto 出库单数据传输对象
 */
export function updateCustomOutbound(dto: CustomOutboundDTO) {
  return httpClient.put<ApiResult<void>>('/wms/custom-outbound', dto)
}

/**
 * 提交出库单（校验可售库存 + 预占，草稿 → 已提交），提交后流转给海外仓作业台下架/打包/签出
 * 库存不足时返回非成功码，data 为缺口明细列表
 * @param id 出库单ID
 */
export function submitCustomOutbound(id: number) {
  return httpClient.patch<ApiResult<StockShortageVO[]>>('/wms/custom-outbound/submit', null, {
    params: { id }
  })
}

/**
 * 取消出库单（草稿直接取消；已提交未下架的释放预占后取消）
 * @param id 出库单ID
 */
export function cancelCustomOutbound(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/custom-outbound/cancel', null, {
    params: { id }
  })
}

/**
 * 删除自定义出库单（仅草稿）
 * @param ids 出库单ID列表
 */
export function deleteCustomOutbound(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/custom-outbound', {
    data: ids
  })
}

/**
 * 导出自定义出库单
 * @param qo 查询条件
 */
export function exportCustomOutbound(qo: CustomOutboundQO) {
  return httpClient.get('/wms/custom-outbound/export', {
    params: qo,
    responseType: 'blob'
  })
}

/**
 * 批量查询可售库存（表单页明细行实时展示所选仓库的可售余量）
 * @param dto 批量查询参数
 */
export function batchQueryAvailableStock(dto: BatchStockQueryDTO) {
  return httpClient.post<ApiResult<Record<string, number>>>(
    '/wms/custom-outbound/available-stock/batch',
    dto
  )
}
