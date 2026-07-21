import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { PositionDTO, PositionPageParam, PositionPageVO } from './types'

/**
 * 岗位管理分页查询
 * @param pageParams 分页参数
 */
export function pagePosition(pageParams: PositionPageParam) {
  return httpClient.get<ApiResult<PositionPageVO>>('/system/position/page', {
    params: pageParams
  })
}

/**
 * 创建岗位管理
 * @param dto
 */
export function createPosition(dto: PositionDTO) {
  return httpClient.post<ApiResult<void>>('/system/position', dto)
}

/**
 * 修改岗位管理
 * @param dto
 */
export function updatePosition(dto: PositionDTO) {
  return httpClient.put<ApiResult<void>>('/system/position', dto)
}

/**
 * 删除岗位管理
 * @param id 主键ID
 */
export function deletePosition(id: number) {
  return httpClient.delete<ApiResult<void>>(`/system/position/` + id)
}
