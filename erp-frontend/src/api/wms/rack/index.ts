import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { RackVO, RackAssignParam, WmsOperatorOption, RackLocation } from './types'

/** 仓库二维预览（按排） */
export function previewRacks(
  warehouseId: number,
  params?: { wmsTenantId?: number; status?: string; expiringSoon?: boolean }
) {
  return httpClient.get<ApiResult<RackVO[]>>('/wms/rack/preview', {
    params: { warehouseId, ...params }
  })
}

/** 分配货架（仅平台超管） */
export function assignRacks(data: RackAssignParam) {
  return httpClient.post<ApiResult<number>>('/wms/rack/assign', data)
}

/** 解除分配（仅平台超管） */
export function unassignRack(id: number) {
  return httpClient.delete<ApiResult<void>>(`/wms/rack/${id}`)
}

/** 某排库位下钻 */
export function rackLocations(warehouseId: number, rackNo: string) {
  return httpClient.get<ApiResult<RackLocation[]>>('/wms/rack/locations', {
    params: { warehouseId, rackNo }
  })
}

/** WMS 服务商选项 */
export function listRackOperators() {
  return httpClient.get<ApiResult<WmsOperatorOption[]>>('/wms/rack/operators')
}
