import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  LocationTransferCreateDTO,
  LocationTransferPageParam,
  LocationTransferPageVO,
  LocationTransferDetailVO,
  LocationTransferPlanDTO,
  TargetLocationVO
} from './types'

/** 库位调整单分页 */
export function pageLocationTransfer(params: LocationTransferPageParam) {
  return httpClient.get<ApiResult<PageResult<LocationTransferPageVO>>>('/wms/location-transfer/page', {
    params
  })
}

/** 库位调整单详情 */
export function getLocationTransferDetail(id: number) {
  return httpClient.get<ApiResult<LocationTransferDetailVO>>('/wms/location-transfer/detail', {
    params: { id }
  })
}

/** 新建库位调整单 */
export function createLocationTransfer(dto: LocationTransferCreateDTO) {
  return httpClient.post<ApiResult<number>>('/wms/location-transfer', dto)
}

/** 调整完成（执行移库） */
export function completeLocationTransfer(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/location-transfer/complete', null, {
    params: { id }
  })
}

export function completeLocationTransferPlan(id: number, dto: LocationTransferPlanDTO) {
  return httpClient.patch<ApiResult<void>>('/wms/location-transfer/plan', dto, {
    params: { id }
  })
}

/** 撤销库位调整单（待调整时） */
export function cancelLocationTransfer(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/location-transfer/cancel', null, {
    params: { id }
  })
}

/** 删除库位调整单（仅已取消） */
export function deleteLocationTransfer(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/location-transfer', { data: ids })
}

/** 目标库位候选（同仓/标准区/服务商租架/空闲或同批可合并） */
export function listTargetCandidates(physicalInventoryId: number) {
  return httpClient.get<ApiResult<TargetLocationVO[]>>('/wms/location-transfer/candidates', {
    params: { physicalInventoryId }
  })
}
