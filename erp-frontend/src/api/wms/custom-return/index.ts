import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  CustomReturnDTO,
  CustomReturnPageParam,
  CustomReturnPageVO,
  CustomReturnDetailVO
} from './types'

/**
 * 自定义退货单分页查询
 * @param pageParams 分页参数
 */
export function pageCustomReturn(pageParams: CustomReturnPageParam) {
  return httpClient.get<ApiResult<PageResult<CustomReturnPageVO>>>('/wms/custom-return/page', {
    params: pageParams
  })
}

/**
 * 获取自定义退货单详情
 * @param id 退货单ID
 */
export function getCustomReturnDetail(id: number) {
  return httpClient.get<ApiResult<CustomReturnDetailVO>>('/wms/custom-return/detail', {
    params: { id }
  })
}

/**
 * 创建自定义退货单
 * @param dto 退货单数据传输对象
 */
export function createCustomReturn(dto: CustomReturnDTO) {
  return httpClient.post<ApiResult<number>>('/wms/custom-return', dto)
}

/**
 * 修改自定义退货单
 * @param dto 退货单数据传输对象
 */
export function updateCustomReturn(dto: CustomReturnDTO) {
  return httpClient.put<ApiResult<void>>('/wms/custom-return', dto)
}

/**
 * 提交退货单（草稿 → 已提交），提交后流转给平台收货/上架
 * @param id 退货单ID
 */
export function submitCustomReturn(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/custom-return/submit', null, {
    params: { id }
  })
}

/**
 * 取消退货单
 * @param id 退货单ID
 */
export function cancelCustomReturn(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/custom-return/cancel', null, {
    params: { id }
  })
}

/**
 * 删除自定义退货单
 * @param ids 退货单ID列表
 */
export function deleteCustomReturn(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/custom-return', {
    data: ids
  })
}

/**
 * 导出自定义退货单
 * @param qo 查询条件
 */
export function exportCustomReturn(qo: CustomReturnPageParam) {
  return httpClient.get('/wms/custom-return/export', {
    params: qo,
    responseType: 'blob'
  })
}
