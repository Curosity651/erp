import type { PageParam } from '@/api/types'

/**
 * 岗位管理
 */
export interface PositionDTO {
  // 岗位ID
  id: number
  // 岗位名称
  name: string
  // 岗位编码，唯一
  code: string
  // 岗位描述
  description: string
  // 显示顺序
  sort: number
  // 状态（1-启用，0-停用）
  status: number
}

export interface PositionQO {
  // 岗位名称
  name?: string
  // 岗位编码，唯一
  code?: string
}

/**
 * 岗位管理分页参数
 */
export type PositionPageParam = PositionQO & PageParam

/**
 * 岗位管理分页视图对象
 */
export interface PositionPageVO extends PositionDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
}
