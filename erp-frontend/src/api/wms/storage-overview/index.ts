import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { StorageOverview, StorageRack, StorageOwner } from './types'

/** 汇总 + 仓库列表（当前登录服务商） */
export function getStorageSummary() {
  return httpClient.get<ApiResult<StorageOverview>>('/wms/storage-overview/summary')
}

/** 某仓货架维度 */
export function getWarehouseRacks(warehouseId: number) {
  return httpClient.get<ApiResult<StorageRack[]>>(`/wms/storage-overview/warehouse/${warehouseId}/racks`)
}

/** 某仓货主维度 */
export function getWarehouseOwners(warehouseId: number) {
  return httpClient.get<ApiResult<StorageOwner[]>>(`/wms/storage-overview/warehouse/${warehouseId}/owners`)
}
