import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  RegionDTO,
  RegionPageParam,
  RegionPageVO,
  RegionOptionVO,
  PlatformRegionMappingVO,
  SavePlatformMappingDTO,
} from './types'

/** 区域分页查询 */
export function pageRegion(params: RegionPageParam) {
  return httpClient.get<ApiResult<PageResult<RegionPageVO>>>('/wms/region/page', { params })
}

/** 区域下拉选项 */
export function listRegionOptions() {
  return httpClient.get<ApiResult<RegionOptionVO[]>>('/wms/region/list')
}

/** 新增区域 */
export function createRegion(data: RegionDTO) {
  return httpClient.post<ApiResult<void>>('/wms/region', data)
}

/** 编辑区域 */
export function updateRegion(data: RegionDTO) {
  return httpClient.put<ApiResult<void>>('/wms/region', data)
}

/** 删除区域 */
export function deleteRegion(id: number) {
  return httpClient.delete<ApiResult<void>>('/wms/region', { params: { id } })
}

/** 查询所有平台映射 */
export function listAllPlatformMappings() {
  return httpClient.get<ApiResult<PlatformRegionMappingVO[]>>('/wms/region/platform-mappings/all')
}

/** 保存平台映射（新增或更新） */
export function savePlatformMapping(data: SavePlatformMappingDTO) {
  return httpClient.post<ApiResult<void>>('/wms/region/platform-mappings/save', data)
}
