/**
 * 项目组查询对象
 */
export interface ProjectGroupQO {
  name?: string
  code?: string
  status?: number
}

/**
 * 项目组列表VO（用于下拉选择）
 */
export interface ProjectGroupListVO {
  id: number
  name: string
  code: string
  status: number
}

import type { PageParam } from '@/api/types'

/**
 * 项目组管理表
 */
export interface ProjectGroupDTO {
  // 项目组ID
  id: number
  // 项目组名称
  name: string
  // 项目组编码，唯一
  code: string
  // 项目组描述
  description: string
  // 状态（1-启用，0-停用）
  status: number
}

export interface ProjectGroupQO {
  // 项目组ID
  id?: number
  // 项目组名称
  name?: string
  // 项目组编码，唯一
  code?: string
  // 项目组描述
  description?: string
  // 状态（1-启用，0-停用）
  status?: number
}

/**
 * 项目组管理表分页参数
 */
export type ProjectGroupPageParam = ProjectGroupQO & PageParam

/**
 * 项目组管理表分页视图对象
 */
export interface ProjectGroupPageVO extends ProjectGroupDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
}
