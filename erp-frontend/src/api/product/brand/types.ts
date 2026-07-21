import type { PageParam } from '@/api/types'

/**
 * 品牌管理表
 */
export interface BrandDTO {
  // 品牌ID
  id: number
  // 品牌名称
  name: string
  // 品牌编码，唯一
  code: string
  // 品牌LOGO URL
  logoUrl: string
  // 品牌原产国家/地区
  originCountry: string
  // 品牌介绍
  description: string
  // 状态（1-启用，0-停用）
  status: number
}

export interface BrandQO {
  // 品牌ID
  id?: number
  // 品牌名称
  name?: string
  // 品牌编码，唯一
  code?: string
  // 品牌LOGO URL
  logoUrl?: string
  // 品牌原产国家/地区
  originCountry?: string
  // 品牌介绍
  description?: string
  // 状态（1-启用，0-停用）
  status?: number
}

/**
 * 品牌管理表分页参数
 */
export type BrandPageParam = BrandQO & PageParam

/**
 * 品牌管理表分页视图对象
 */
export interface BrandPageVO extends BrandDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
}

/**
 * 品牌查询对象
 */
export interface BrandQO {
  name?: string
  code?: string
  status?: number
}

/**
 * 品牌列表VO（用于下拉选择）
 */
export interface BrandListVO {
  id: number
  name: string
  code: string
  status: number
}
