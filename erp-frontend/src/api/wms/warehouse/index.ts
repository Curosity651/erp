import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  WarehouseDTO,
  WarehouseOptionVO,
  WarehousePageParam,
  WarehousePageVO,
  FboWarehouseVO,
  FboWarehouseImportDTO,
  FboWarehouseImportResultVO
} from './types'

/**
 * 仓库分页查询
 * @param pageParams 分页参数
 */
export function pageWarehouse(pageParams: WarehousePageParam) {
  return httpClient.get<ApiResult<WarehousePageVO>>('/wms/warehouse/page', {
    params: pageParams
  })
}

/**
 * 创建仓库
 * @param dto
 */
export function createWarehouse(dto: WarehouseDTO) {
  return httpClient.post<ApiResult<void>>('/wms/warehouse', dto)
}

/**
 * 修改仓库
 * @param dto
 */
export function updateWarehouse(dto: WarehouseDTO) {
  return httpClient.put<ApiResult<void>>('/wms/warehouse', dto)
}

/**
 * 获取仓库下拉选项列表
 */
export function getWarehouseOptions() {
  return httpClient.get<ApiResult<WarehouseOptionVO[]>>('/wms/warehouse/options')
}

/**
 * 更新仓库状态
 * @param id 仓库ID
 * @param status 状态: 1-启用 / 0-停用
 */
export function updateWarehouseStatus(id: number, status: number) {
  return httpClient.patch<ApiResult<void>>('/wms/warehouse/status', { id, status })
}

/**
 * 获取仓库详情
 * @param id 仓库ID
 */
export function getWarehouseDetail(id: number) {
  return httpClient.get<ApiResult<WarehousePageVO>>('/wms/warehouse/detail', { params: { id } })
}
