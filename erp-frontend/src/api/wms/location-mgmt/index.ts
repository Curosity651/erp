import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  WarehousePalletRuleUpdate,
  WarehouseStructure,
  WarehouseStructureUpdate,
  WmsZone,
  WmsLocation
} from './types'

/** 自有仓库列表（含结构参数） */
export function listStructureWarehouses() {
  return httpClient.get<ApiResult<WarehouseStructure[]>>('/wms/location-mgmt/warehouses')
}

/** 更新仓库结构参数 */
export function updateWarehouseStructure(data: WarehouseStructureUpdate) {
  return httpClient.patch<ApiResult<number>>('/wms/location-mgmt/structure', data)
}

/** 更新托盘规则，不重新生成库位或托位 */
export function updateWarehousePalletRules(data: WarehousePalletRuleUpdate) {
  return httpClient.patch<ApiResult<void>>('/wms/location-mgmt/pallet-rules', data)
}

/** 仓库分区列表 */
export function listZones(warehouseId: number) {
  return httpClient.get<ApiResult<WmsZone[]>>('/wms/location-mgmt/zones', {
    params: { warehouseId }
  })
}

/** 初始化默认四类分区 */
export function initDefaultZones(warehouseId: number) {
  return httpClient.post<ApiResult<number>>('/wms/location-mgmt/zones/init-defaults', undefined, {
    params: { warehouseId }
  })
}

/** 批量生成库位 */
export function generateLocations(warehouseId: number) {
  return httpClient.post<ApiResult<number>>('/wms/location-mgmt/generate', undefined, {
    params: { warehouseId }
  })
}

/** 仓库库位列表 */
export function listLocations(warehouseId: number) {
  return httpClient.get<ApiResult<WmsLocation[]>>('/wms/location-mgmt/locations', {
    params: { warehouseId }
  })
}

/** 该仓有货占用的库位编码集合（锁定有货格子改分区用） */
export function listOccupiedLocations(warehouseId: number) {
  return httpClient.get<ApiResult<string[]>>('/wms/location-mgmt/occupied-locations', {
    params: { warehouseId }
  })
}

/** 库位改分区（联动更新现有批次可分配性并重算可用库存） */
export function moveLocationZone(data: {
  warehouseId: number
  locationIds: number[]
  zoneId: number
}) {
  return httpClient.post<ApiResult<number>>('/wms/location-mgmt/locations/move-zone', data)
}

/** 虚拟库位列表（收纳积压货用，服务商不可见） */
export function listVirtualLocations(warehouseId: number) {
  return httpClient.get<ApiResult<WmsLocation[]>>('/wms/location-mgmt/virtual-locations', {
    params: { warehouseId }
  })
}

/** 新增虚拟库位（自定义名称） */
export function createVirtualLocation(warehouseId: number, name: string) {
  return httpClient.post<ApiResult<WmsLocation>>('/wms/location-mgmt/virtual-locations', undefined, {
    params: { warehouseId, name }
  })
}

/** 删除虚拟库位（无货时） */
export function deleteVirtualLocation(id: number) {
  return httpClient.delete<ApiResult<void>>('/wms/location-mgmt/virtual-locations', {
    params: { id }
  })
}
