import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 库存流水查询对象
 */
export interface StockFlowQO {
  // 仓库ID
  warehouseId?: number
  // SKU编码
  skuCode?: string
  // 来源单据类型
  sourceType?: string
  // 来源单据号
  sourceNo?: string
  // 创建时间开始
  createTimeStart?: string
  // 创建时间结束
  createTimeEnd?: string
  /** 过账类型列表（多选） */
  postingTypes?: string[]
  /** 方向: IN/OUT */
  direction?: string
}

/**
 * 库存流水分页参数
 */
export type StockFlowPageParam = StockFlowQO & PageParam

/**
 * 库存桶类型
 */
export type BucketType = 'AVAILABLE' | 'RESERVED' | 'IN_TRANSIT'

/**
 * 库存方向
 */
export type StockDirection = 'IN' | 'OUT'

/**
 * 仓库展示信息
 */
export interface WarehouseDisplayVO {
  warehouseId: number
  warehouseName: string
  warehouseType?: string
}

/**
 * 区域展示信息
 */
export interface RegionDisplayVO {
  regionId: number
  regionCode: string
  regionName: string
}

/**
 * 库存流水分页VO
 */
export interface StockFlowPageVO {
  // 流水ID
  id: number
  // 仓库ID（仓库级流水时 > 0，区域级流水时 = 0）
  warehouseId: number
  // 仓库展示信息
  warehouseDisplay?: WarehouseDisplayVO
  // 区域ID（区域级流水时 > 0，仓库级流水时 = 0）
  regionId: number
  // 区域展示信息
  regionDisplay?: RegionDisplayVO
  // SKU编码
  skuCode: string
  // SKU展示信息
  skuBrief?: SkuBriefVO
  // 库存桶: AVAILABLE/RESERVED/IN_TRANSIT
  bucket: BucketType
  // 方向: IN/OUT
  direction: StockDirection
  // "变更数量(正数)"
  quantity: number
  // 变动前数量
  beforeQuantity: number
  // 变动后数量
  afterQuantity: number
  // 过账单ID
  postingId?: number
  // 过账单号
  postingNo?: string
  // 过账单明细ID
  postingItemId?: number
  // 过账类型
  postingType: string
  // 来源单据类型
  sourceType?: string
  // 来源单据ID
  sourceId?: number
  // 来源单据号
  sourceNo?: string
  // 备注
  remark?: string
  // 操作人ID
  createBy?: number
  // 创建时间
  createTime: string
}

/**
 * 库存流水详情VO
 */
export interface StockFlowDetailVO extends StockFlowPageVO {
  // 仓库类型
  warehouseType: string
  // 来源单据ID
  sourceId?: number
}

/**
 * 今日汇总统计VO
 */
export interface StockFlowTodaySummaryVO {
  // 今日入库数量
  inQuantity: number
  // 今日出库数量
  outQuantity: number
  // 今日预占数量
  reserveQuantity: number
  // 今日释放数量
  releaseQuantity: number
}

/**
 * 流水趋势数据VO
 */
export interface StockFlowTrendVO {
  // 日期
  date: string
  // 入库数量
  inQuantity: number
  // 出库数量
  outQuantity: number
}
