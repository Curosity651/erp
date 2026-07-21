import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'
import type { WarehouseDisplayVO, RegionDisplayVO } from '@/api/wms/stock-flow/types'

/** 过账单查询参数 */
export interface StockPostingPageParam extends PageParam {
  postingNo?: string
  warehouseId?: number
  postingType?: string
  sourceType?: string
  sourceNo?: string
  postTimeStart?: string
  postTimeEnd?: string
}

/** 过账单分页 VO */
export interface StockPostingPageVO {
  id: number
  postingNo: string
  warehouseId: number
  warehouseName?: string
  warehouseDisplay?: WarehouseDisplayVO
  regionId: number
  regionDisplay?: RegionDisplayVO
  postingType: string
  postingTypeDesc?: string
  sourceType?: string
  sourceId?: number
  sourceNo?: string
  relatedPostingId?: number
  bizTime?: string
  postTime?: string
  remark?: string
  skuCount?: number
  totalQuantity?: number
  createBy?: number
  createTime?: string
}

/** 过账单明细 VO（完整字段，用于详情展示） */
export interface StockPostingItemVO {
  id: number
  postingId: number
  warehouseId: number
  regionId: number
  skuCode: string
  skuBrief?: SkuBriefVO
  bucket: string
  direction: string
  quantity: number
  warehouseDisplay?: WarehouseDisplayVO
  regionDisplay?: RegionDisplayVO
}

/** 过账单详情 VO */
export interface StockPostingDetailVO extends StockPostingPageVO {
  items: StockPostingItemVO[]
}

/** 过账单明细查询参数 */
export interface StockPostingItemPageParam extends PageParam {
  postingId: number
  skuCode?: string
}

/** 过账单明细分页 VO */
export interface StockPostingItemPageVO {
  id: number
  postingId: number
  warehouseId: number
  regionId: number
  skuCode: string
  skuBrief?: SkuBriefVO
  bucket: string
  direction: string
  quantity: number
  warehouseDisplay?: WarehouseDisplayVO
  regionDisplay?: RegionDisplayVO
}
