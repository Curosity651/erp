import type { PageParam } from '@/api/types'

/**
 * SKU映射
 */
export interface SkuMappingDTO {
  id?: number
  platformItemId?: string
  skuCode?: string
}

export interface SkuMappingQO {
  // 平台商品ID
  platformItemId?: string
  // ERP SKU 编码
  skuCode?: string
}

/**
 * SKU映射分页参数
 */
export type SkuMappingPageParam = SkuMappingQO & PageParam

/**
 * SKU映射分页视图对象
 */
export interface SkuMappingPageVO extends SkuMappingDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
  // SKU中文名
  skuChineseName?: string
  // 品类名称
  categoryName?: string
  // 店铺名称
  shopName?: string
}

/**
 * SKU映射列表项 - 用于映射详情展示
 */
export interface SkuMappingListItem {
  // 主键ID
  id: number
  // 平台商品ID
  platformItemId: string
  // SKU编码
  skuCode: string
  // 创建时间
  createTime: string
}

/**
 * 快速创建映射DTO
 */
export interface SkuMappingQuickCreateDTO {
  // SKU编码
  skuCode: string
  // 平台商品ID
  platformItemId: string
}
