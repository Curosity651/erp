import type { PageParam } from '@/api/types'

/**
 * 商品品类
 */
export interface CategoryDTO {
  // 品类ID
  id: number
  // 品类名称
  name: string
  // 品类编码，唯一
  code: string
  // 上级品类ID，0为顶级
  parentId: number
  // 品类层级（1级/2级/3级等）
  level: number
  // 排序
  sort: number
  // 状态（1-启用，0-停用）
  status: number
}

export interface CategoryQO {
  // 品类ID
  id?: number
  // 品类名称
  name?: string
  // 品类编码，唯一
  code?: string
  // 上级品类ID，0为顶级
  parentId?: number
  // 品类层级（1级/2级/3级等）
  level?: number
  // 排序
  sort?: number
  // 状态（1-启用，0-停用）
  status?: number
}

/**
 * 商品品类分页参数
 */
export type CategoryPageParam = CategoryQO & PageParam

/**
 * 商品品类分页视图对象
 */
export interface CategoryPageVO extends CategoryDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
}
