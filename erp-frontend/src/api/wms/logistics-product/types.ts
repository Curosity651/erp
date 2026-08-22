/**
 * WMS 服务商物流产品 类型契约。
 * 服务商定义（特性词条 + 统一单价）提供给名下货主；货主建单时选用，签出时按单价计一次费。
 */

/** 物流产品（管理页 + 货主建单选项共用） */
export interface LogisticsProductVO {
  id: number
  productName: string
  productCode?: string
  // 特性词条（大件/小件/自提/自定义...）
  tags: string[]
  // 统一单价（每次使用）
  unitPrice: number
  currency: string
  productDescription?: string
  // 1启用 / 0停用
  status: number
  remark?: string
  createTime?: string
}

/** 新建/编辑入参 */
export interface LogisticsProductDTO {
  id?: number
  productName: string
  productCode?: string
  tags?: string[]
  unitPrice: number
  currency: string
  productDescription?: string
  remark?: string
}

/** 预置特性词条（可自定义输入追加） */
export const PRESET_TAGS = ['大件', '小件', '自提', '经济', '加急', '易碎', '冷链', '上门配送']
