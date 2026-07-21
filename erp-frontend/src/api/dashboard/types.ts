/**
 * Dashboard API类型定义
 */

// 查询参数
export interface DashboardQueryParams {
  startDate: string // YYYY-MM-DD
  endDate: string // YYYY-MM-DD
  platform?: string // all | wildberries | ozon
  skuCodes?: string[] // SKU编码列表（可多选）
  shopIds?: number[] // 店铺ID列表（可多选）
  categoryId?: number // 品类ID
}

// 筛选条件（前端使用）
export interface DashboardFilters {
  startDate: string
  endDate: string
  platform: string
  skuCodes?: string[] // SKU编码列表（可多选）
  shopIds?: number[] // 店铺ID列表（可多选）
  categoryId?: number // 品类ID
}

// Dashboard数据VO
export interface DashboardDataVO {
  salesOverview: SalesOverviewVO
  targetProgress: TargetProgressVO
  exchangeRates: ExchangeRatesVO
  platformFulfillment: PlatformFulfillmentVO[]
  salesTrend: SalesTrendVO
  orderStatus: OrderStatusVO[]
  skuRanking: SkuRankingVO
}

// 销售概览
export interface SalesOverviewVO {
  totalSales: number
}

// 目标进度
export interface TargetProgressVO {
  monthly: TargetDetailVO
  yearly: TargetDetailVO
}

export interface TargetDetailVO {
  target: number
  current: number
  progress: number
  hasTarget: boolean
}

// 汇率信息
export interface ExchangeRatesVO {
  date: string
  rates: RateItem[]
}

export interface RateItem {
  currency: string
  rate: number
}

// 平台履约分布
export interface PlatformFulfillmentVO {
  platform: string
  fbs: FulfillmentDetail
  fbo: FulfillmentDetail
  total: FulfillmentDetail
}

export interface FulfillmentDetail {
  count: number
  amount: number
}

// 销售趋势
export interface SalesTrendVO {
  granularity: 'hour' | 'day'
  data: TrendPoint[]
}

export interface TrendPoint {
  time: string
  totalSales: number
  effectiveSales: number
}

// 订单状态分布
export interface OrderStatusVO {
  status: string
  count: number
  amount: number
}

// SKU排名
export interface SkuRankingVO {
  top5: SkuItem[]
  bottom5: SkuItem[]
}

export interface SkuItem {
  sku: string
  isMapped: boolean
  quantity: number
  amount: number
}

// SKU排名查询参数
export interface SkuRankingQueryParams {
  startDate: string // YYYY-MM-DD
  endDate: string // YYYY-MM-DD
  platform?: string // all | wildberries | ozon
  skuCodes?: string[] // SKU编码列表
  shopIds?: number[] // 店铺ID列表
  categoryId?: number // 品类ID
}

// SKU排名分页请求
export interface SkuRankingPageParams {
  current: number
  size: number
}

// SKU排名项（包含完整信息）
export interface SkuRankingItemVO {
  rank: number // 排名
  sku: string // SKU编码
  isMapped: boolean // 是否已映射
  quantity: number // 销量
  amount: number // 销售额
  skuId?: number // SKU ID
  skuNameCn?: string // 产品中文名称
  imageUrl?: string // 产品图片
  categoryId?: number // 品类ID
  categoryName?: string // 品类名称
}
