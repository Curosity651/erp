import type { SkuBriefVO } from '@/api/common/sku-types'

/** 全局库存配置 */
export interface GlobalInventoryConfigVO {
  safetyStock: number
  notifyEnabled: boolean
  notifyThresholdDays: number
  notifyUserIds: number[]
  notifyUserNames: string[]
}

export interface GlobalInventoryConfigDTO {
  safetyStock: number
  notifyEnabled: boolean
  notifyThresholdDays: number
  notifyUserIds: number[]
}

/** SKU 独立配置 */
export interface InventoryConfigVO {
  id: number
  regionId: number
  regionName: string
  skuCode: string
  skuBrief: SkuBriefVO
  safetyStock: number | null
  notifyEnabled: boolean
  notifyThresholdDays: number | null
  notifyThresholdDisplay: string
  createTime: string
}

export interface InventoryConfigDTO {
  id?: number
  regionId: number
  skuCode: string
  safetyStock?: number | null
  notifyEnabled: boolean
  notifyThresholdDays?: number | null
}

export interface InventoryConfigQO {
  regionId?: number
  skuKeyword?: string
}

/** 库存预测汇总 */
export type ForecastStatus = 'SUFFICIENT' | 'LOW' | 'CRITICAL' | 'STOCKOUT' | 'NO_SALES'

export interface ForecastStatusCounts {
  SUFFICIENT: number
  LOW: number
  CRITICAL: number
  STOCKOUT: number
  NO_SALES: number
}

export interface ForecastSummaryVO {
  skuCode: string
  skuBrief: SkuBriefVO
  regionId: number
  regionName: string
  availableQuantity: number
  /** 预占库存（区域级） */
  reservedQuantity: number
  /** 可售库存 = 可用 - 预占 */
  sellableQuantity: number
  inTransitQuantity: number
  /** 待发货库存（采购已确认未发货） */
  pendingShipmentQuantity: number
  dailySales: number
  sellableDays: number | null
  stockoutDate: string | null
  status: ForecastStatus
  /** 有效安全库存 = max(safetyStock, dailySales × thresholdDays) */
  effectiveSafetyStock: number
}

export interface ForecastSummaryQO {
  regionId?: number
  skuKeyword?: string
  status?: ForecastStatus
  days?: number
}

export interface ForecastSummaryResult {
  total: number
  statusCounts: ForecastStatusCounts
  thresholdDays: number
  list: ForecastSummaryVO[]
}

/** SKU 预测详情 */
export interface CurrentStockVO {
  available: number
  reserved: number
  /** 可售库存 = 可用 - 预占 */
  sellable: number
  inTransit: number
  /** 待发货库存（采购已确认未发货） */
  pendingShipment: number
}

/** 入库明细类型 */
export type IncomingType = 'IN_TRANSIT' | 'PENDING_SHIPMENT'

export interface IncomingDetailVO {
  /** 类型: IN_TRANSIT-在途, PENDING_SHIPMENT-待发货 */
  type: IncomingType
  /** 来源单号（物流单号或采购单号） */
  sourceNo: string
  quantity: number
  /** 状态（运输中/生产中/待发货） */
  status: string
}

export interface ForecastDayVO {
  date: string
  openingStock: number
  incoming: number
  sales: number
  closingStock: number
  status: ForecastStatus
  incomingDetails: IncomingDetailVO[]
}

/** 入库来源明细（区域级） */
export interface IncomingSourceVO {
  /** 类型: LOGISTICS-物流 */
  type: string
  /** 来源单号 */
  sourceNo: string
  skuCode: string
  quantity: number
  /** 预计到货日期 */
  expectedDate: string | null
}

export interface ForecastDetailVO {
  skuBrief: SkuBriefVO
  regionId: number
  regionName: string
  currentStock: CurrentStockVO
  safetyStock: number
  dailySales: number
  sellableDays: number | null
  /** 库存状态（后端统一计算） */
  status: ForecastStatus
  thresholdDays: number
  forecastList: ForecastDayVO[]
  /** 入库来源明细 */
  incomingSources: IncomingSourceVO[]
  /** 有效安全库存 */
  effectiveSafetyStock: number
  /** 待发货总量（不含在预测中） */
  pendingShipmentTotal: number
}

export interface ForecastDetailQO {
  regionId: number
  skuCode: string
  days?: number
  dailySales?: number
}

/** 入库计划 */
export interface IncomingPlanVO {
  shippingNo: string
  estimatedArrivalDate: string
  quantity: number
}
