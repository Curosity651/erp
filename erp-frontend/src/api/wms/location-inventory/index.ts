import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  LocationInventoryDetail,
  LocationInventoryGrid,
  LocationInventoryTree
} from './types'

export function getLocationInventoryGrid(warehouseId: number) {
  return httpClient.get<ApiResult<LocationInventoryGrid[]>>('/wms/location-inventory/grid', {
    params: { warehouseId }
  })
}

export function getLocationInventoryTree(warehouseId: number) {
  return httpClient.get<ApiResult<LocationInventoryTree>>('/wms/location-inventory/tree', {
    params: { warehouseId }
  })
}

export function getLocationInventoryDetail(locationId: number) {
  return httpClient.get<ApiResult<LocationInventoryDetail>>(
    `/wms/location-inventory/location/${locationId}`
  )
}
