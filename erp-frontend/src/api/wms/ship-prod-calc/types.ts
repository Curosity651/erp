import type { SkuBriefVO } from '@/api/common/sku-types'

/** 决策筛选 */
export type ShipProdDecision = 'SHIP' | 'PRODUCE' | 'SHORTAGE' | 'NO_SALES'

/** 查询条件 */
export interface ShipProdCalcQO {
  skuKeyword?: string
  decision?: ShipProdDecision
  /** 测算基准日 YYYY-MM-DD（默认今天） */
  baseDate?: string
}

/** 状态卡计数 */
export interface ShipProdCounts {
  needShip: number
  needProduce: number
  shortage: number
  noSales: number
}

/** 测算汇总行（一行一个 SKU） */
export interface ShipProdCalcRowVO {
  skuCode: string
  skuBrief?: SkuBriefVO
  /** A+B 现货 */
  onHand: number
  /** C 在途 */
  inTransit: number
  /** D+E 在制 */
  producing: number
  xt: number
  yt: number
  zt: number
  /** 现货支撑天数（null = ∞） */
  shipSupportDays: number | null
  /** 发货全链路支撑天数 */
  totalSupportDays: number | null
  /** 生产全链路支撑天数 */
  prodSupportDays: number | null
  needShip: boolean
  shipPlanQty: number
  shipPath: string
  needProduce: boolean
  prodPlanQty: number
  prodPath: string
  shortage: boolean
  noSales: boolean
  warnings: string[]
}

/** 汇总返回 */
export interface ShipProdCalcSummaryVO {
  total: number
  counts: ShipProdCounts
  shipThresholdDays: number
  prodThresholdDays: number
  safetyStockDays: number
  list: ShipProdCalcRowVO[]
}

/** 批次 */
export interface ShipProdBatchVO {
  qty: number
  eta: string | null
  label: string
}

/** 单 SKU 详情（数据链） */
export interface ShipProdCalcDetailVO {
  skuCode: string
  skuBrief?: SkuBriefVO
  baseDate: string

  overseas: number
  fbo: number
  inTransit: number
  factoryDone: number
  producing: number

  xt: number
  yt: number
  zt: number
  zt1: number
  zt2: number
  zt3: number
  avg7: number
  avg15: number
  avg30: number
  kBase: number
  peakSamples: number
  historyDays: number

  shipSupportDays: number | null
  shipDtC: number | null
  totalSupportDays: number | null
  needShip: boolean
  shipPlanQty: number
  shipPath: string

  prodSupportDays: number | null
  prodDtE: number | null
  prodTotalSupportDays: number | null
  needProduce: boolean
  prodPlanQty: number
  prodPath: string

  earliestArrivalDate: string | null
  earliestCompletionDate: string | null
  stockoutDate: string | null
  shipRedLineDate: string
  prodRedLineDate: string

  transitBatches: ShipProdBatchVO[]
  producingBatches: ShipProdBatchVO[]

  warnings: string[]
}
