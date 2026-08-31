import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  LocationTransferPageParam,
  LocationTransferPageVO,
  LocationTransferDetailVO,
  LogicalLocationTransferCreateDTO,
  LogicalTransferLocationVO,
  LogicalTransferSourceVO,
  LocationTransferSourceBatchVO,
  TargetLocationVO
} from './types'
import type { WmsLocation } from '@/api/wms/location-mgmt/types'

/** 库位调整单分页 */
export function pageLocationTransfer(params: LocationTransferPageParam) {
  return httpClient.get<ApiResult<PageResult<LocationTransferPageVO>>>(
    '/wms/location-transfer/page',
    {
      params
    }
  )
}

/** 库位调整单详情 */
export function getLocationTransferDetail(id: number) {
  return httpClient.get<ApiResult<LocationTransferDetailVO>>('/wms/location-transfer/detail', {
    params: { id }
  })
}

export function createLogicalLocationTransfer(dto: LogicalLocationTransferCreateDTO) {
  return httpClient.post<ApiResult<number>>('/wms/location-transfer/logical', dto)
}

export function listLogicalTransferSources(params: {
  warehouseId: number
  locationId?: number
  erpTenantId?: number
  skuKeyword?: string
}) {
  return httpClient.get<ApiResult<LogicalTransferSourceVO[]>>(
    '/wms/location-transfer/logical-sources',
    { params }
  )
}

export function listLogicalTransferTargets(params: {
  warehouseId: number
  erpTenantId: number
}) {
  return httpClient.get<ApiResult<LogicalTransferLocationVO[]>>(
    '/wms/location-transfer/logical-targets',
    { params }
  )
}

export function listLocationTransferSources(params: {
  warehouseId: number
  locationCode?: string
  erpTenantId?: number
  skuKeyword?: string
  palletNo?: string
}) {
  return httpClient.get<ApiResult<LocationTransferSourceBatchVO[]>>(
    '/wms/location-transfer/sources',
    { params }
  )
}

export function listSelectableTransferLocations(params: {
  warehouseId: number
  erpTenantId?: number
  sourceLocationCode?: string
}) {
  return httpClient.get<ApiResult<WmsLocation[]>>('/wms/location-transfer/selectable-locations', {
    params
  })
}

/** 调整完成（执行移库） */
export function completeLocationTransfer(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/location-transfer/logical-complete', null, {
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
export function listTargetCandidates(
  physicalInventoryId: number,
  moveMode: 'PARTIAL' | 'WHOLE_PALLET' = 'PARTIAL',
  targetLocationCode?: string
) {
  return httpClient.get<ApiResult<TargetLocationVO[]>>('/wms/location-transfer/candidates', {
    params: { physicalInventoryId, moveMode, targetLocationCode }
  })
}
