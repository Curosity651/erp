import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  ProjectGroupDTO,
  ProjectGroupListVO,
  ProjectGroupPageParam,
  ProjectGroupPageVO
} from './types'

/**
 * 项目组管理表分页查询
 * @param pageParams 分页参数
 */
export function pageProjectGroup(pageParams: ProjectGroupPageParam) {
  return httpClient.get<ApiResult<ProjectGroupPageVO>>('/system/project-group/page', {
    params: pageParams
  })
}

/**
 * 创建项目组管理表
 * @param dto
 */
export function createProjectGroup(dto: ProjectGroupDTO) {
  return httpClient.post<ApiResult<void>>('/system/project-group', dto)
}

/**
 * 修改项目组管理表
 * @param dto
 */
export function updateProjectGroup(dto: ProjectGroupDTO) {
  return httpClient.put<ApiResult<void>>('/system/project-group', dto)
}

/**
 * 删除项目组管理表
 * @param id 主键ID
 */
export function deleteProjectGroup(id: number) {
  return httpClient.delete<ApiResult<void>>(`/system/project-group/` + id)
}

/**
 * 获取项目组列表（用于下拉选择）
 */
export function getProjectGroupList() {
  return httpClient.get<ApiResult<ProjectGroupListVO[]>>('/system/project-group/list')
}
