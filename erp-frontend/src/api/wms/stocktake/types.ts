import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 盘点范围枚举
 */
export type StocktakeScope = 'ALL' | 'PARTIAL'

export type StocktakeMode = 'FULL' | 'CYCLE' | 'SPECIAL'

/**
 * 盘点单状态枚举
 */
export type StocktakeStatus =
  | 'DRAFT'
  | 'READY'
  | 'COUNTING'
  | 'REVIEWING'
  | 'CONFIRMED'
  | 'CANCELLED'

export type StocktakeTaskStatus =
  | 'PENDING'
  | 'COUNTING'
  | 'COMPLETED'
  | 'RECOUNTING'
  | 'REVIEWED'

/**
 * 盘点明细状态枚举
 */
export type StocktakeItemStatus = 'PENDING' | 'COUNTED'

/**
 * 盘点明细SKU来源
 */
export type StocktakeItemSource = 'EXISTING' | 'ADDED'

/**
 * SKU来源描述映射
 */
export const StocktakeItemSourceMap: Record<StocktakeItemSource, string> = {
  EXISTING: '库内',
  ADDED: '追加'
}

/**
 * 盘点单数据传输对象
 */
export interface StocktakeDTO {
  // 主键ID (编辑时必填)
  id?: number
  // 盘点仓库ID
  warehouseId: number
  // 盘点日期
  stocktakeDate: string
  // 盘点范围: ALL-全部SKU / PARTIAL-指定SKU
  stocktakeScope: StocktakeScope
  stocktakeMode: StocktakeMode
  locationIds?: number[]
  specialSkuCodes?: string[]
  specialOwnerId?: number
  specialSearchAll?: boolean
  virtualLocationOnly?: boolean
  blindCount?: boolean
  // 备注
  remark?: string
}

/**
 * 盘点录入数据传输对象
 */
export interface StocktakeItemsDTO {
  // 盘点单ID
  stocktakeId: number
  // 盘点明细列表
  items: StocktakeItemInputDTO[]
}

/**
 * 盘点明细录入数据传输对象
 */
export interface StocktakeItemInputDTO {
  // 明细ID
  itemId: number
  // 实盘数量（null表示清除）
  actualQuantity: number | null
}

/**
 * 盘点单查询对象
 */
export interface StocktakeQO {
  // 盘点单号
  stocktakeNo?: string
  // 盘点仓库ID
  warehouseId?: number
  // 盘点单状态
  orderStatus?: StocktakeStatus
  // 盘点日期开始
  stocktakeDateStart?: string
  // 盘点日期结束
  stocktakeDateEnd?: string
}

/**
 * 盘点单分页参数
 */
export type StocktakePageParam = StocktakeQO & PageParam

/**
 * 盘点单分页视图对象
 */
export interface StocktakePageVO {
  // 主键ID
  id: number
  // 盘点单号
  stocktakeNo: string
  // 盘点仓库ID
  warehouseId: number
  // 盘点仓库名称
  warehouseName: string
  // 盘点日期
  stocktakeDate: string
  // 盘点范围
  stocktakeScope: StocktakeScope
  stocktakeMode?: StocktakeMode
  freezeMode?: 'WAREHOUSE' | 'LOCATION'
  blindCount?: number
  locationCount?: number
  completedLocationCount?: number
  // 盘点单状态
  orderStatus: StocktakeStatus
  // SKU数量
  skuCount: number
  // 差异数量
  diffCount: number
  // 已盘点梳理
  countedCount: number
  // 确认时间
  confirmTime?: string
  // 确认人名称
  confirmByName?: string
  // 实际盘点操作人，多人以顿号分隔
  operatorNames?: string
  // 创建人名称
  createByName?: string
  // 创建时间
  createTime: string
}

/**
 * 盘点单详情视图对象
 */
export interface StocktakeDetailVO extends StocktakePageVO {
  // 备注
  remark?: string
  // 盘点明细列表
  items: StocktakeItemVO[]
}

/**
 * 盘点明细视图对象
 */
export interface StocktakeItemVO {
  // 明细ID
  id: number
  locationTaskId?: number
  physicalInventoryId?: number
  sourceInventoryId?: number
  palletId?: number
  slotId?: number
  capacityPercent?: number
  manualFull?: number
  // 货主ID（同一skuCode在不同货主下可重名，须以货主+skuCode区分）
  erpTenantId?: number
  // 货主名称
  ownerName?: string
  wmsTenantId?: number
  // SKU编码
  skuCode: string
  warehouseSkuCode?: string
  zoneId?: number
  zoneName?: string
  zoneType?: string
  locationCode?: string
  slotCode?: string
  quality?: string
  allocatable?: number
  inboundDate?: string
  pickOrder?: number
  // SKU展示信息
  skuBrief?: SkuBriefVO
  // 系统数量
  systemQuantity: number
  reservedQuantity?: number
  // 实盘数量
  actualQuantity?: number
  // 差异数量
  diffQuantity?: number
  // 盘点状态
  stocktakeStatus: StocktakeItemStatus
  // SKU来源: EXISTING-库内 / ADDED-追加
  sourceType?: StocktakeItemSource
  // SKU来源描述
  sourceTypeDesc?: string
  reviewStatus?: string
  // 备注
  remark?: string
}

export interface StocktakeLocationTaskVO {
  id: number
  stocktakeOrderId: number
  warehouseId: number
  zoneId?: number
  locationId: number
  locationCode: string
  isVirtual?: number
  taskStatus: StocktakeTaskStatus
  assigneeName?: string
  itemCount: number
  countedCount: number
  diffCount: number
  completedTime?: string
}

export interface StocktakeExtraItemDTO {
  taskId: number
  erpTenantId: number
  skuCode: string
  actualQuantity: number
  slotCode?: string
  palletId?: number
  capacityPercent?: number
  manualFull?: boolean
  quality?: string
  inboundDate?: string
  remark?: string
}

/**
 * 盘点进度视图对象
 */
export interface StocktakeProgressVO {
  // 总SKU数
  totalCount: number
  // 已盘点数
  countedCount: number
  // 未盘点数
  pendingCount: number
  // 进度百分比
  progressPercent: number
  // 盘盈项数
  profitCount: number
  // 盘盈数量
  profitQuantity: number
  // 盘亏项数
  lossCount: number
  // 盘亏数量
  lossQuantity: number
  // 净差异
  netDiff: number
}

/**
 * 盘点差异预览视图对象
 */
export interface StocktakeDiffPreviewVO {
  // 盘点明细总数
  totalCount: number
  // 无差异数量
  noDiffCount: number
  // 盘盈SKU数量（正差异）
  profitCount: number
  // 盘盈总数量
  profitQuantity: number
  // 盘亏SKU数量（负差异）
  lossCount: number
  // 盘亏总数量
  lossQuantity: number
  // 未盘点数量
  pendingCount: number
  // 差异明细列表（仅包含有差异的项）
  diffItems: StocktakeDiffItemVO[]
}

/**
 * 盘点差异明细项视图对象
 */
export interface StocktakeDiffItemVO {
  // SKU编码
  skuCode: string
  // SKU展示信息
  skuBrief?: SkuBriefVO
  // 系统数量
  systemQuantity: number
  // 实盘数量
  actualQuantity: number
  // 差异数量
  diffQuantity: number
  // 差异类型: PROFIT-盘盈 / LOSS-盘亏
  diffType: 'PROFIT' | 'LOSS'
}

/**
 * 可用SKU视图对象
 */
export interface AvailableSkuVO {
  skuCode: string
  warehouseSkuCode?: string
  erpTenantId?: number
  ownerName?: string
  skuBrief?: SkuBriefVO
  stockQuantity: number
}

/**
 * 可盘点SKU预览VO
 */
export interface AvailableSkuPreviewVO {
  totalCount: number
  items: AvailableSkuVO[]
}

/**
 * 追加盘点SKU请求
 */
export interface StocktakeAddSkuDTO {
  stocktakeId: number
  /** 货主（货物归属），追加的这批SKU归属该货主 */
  erpTenantId: number
  skuCodes: string[]
}
