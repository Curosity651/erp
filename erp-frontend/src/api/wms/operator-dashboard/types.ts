/**
 * WMS 服务商运营数据分析看板（900400）类型契约。
 * A 经营总览 / B 收支趋势(按月) / C 产品分析 / D 货主分析 / E 服务规模。
 */

/** 查询入参 */
export interface OperatorDashboardParams {
  // 账期起止 YYYY-MM（缺省=最近6个月）
  monthStart?: string
  monthEnd?: string
  // 名下货主过滤（多选，空=全部）
  erpTenantIds?: number[]
}

/** A 经营总览 */
export interface OverviewVO {
  income: number
  incomeCount: number
  expense: number
  netProfit: number
  ownerTotal: number
  ownerEnabled: number
  productTotal: number
  productEnabled: number
}

/** B 收支趋势（按月，数组与 months 对齐） */
export interface TrendVO {
  months: string[]
  income: number[]
  expense: number[]
  net: number[]
}

/** C 产品统计（柱图取 usageCount，环形取 amount） */
export interface ProductStatVO {
  productId: number
  productName: string
  usageCount: number
  amount: number
}

/** D 货主收入贡献 */
export interface OwnerStatVO {
  erpTenantId: number
  ownerName: string
  amount: number
  usageCount: number
}

/** D 货主出库吞吐 */
export interface OwnerOrderStatVO {
  erpTenantId: number
  ownerName: string
  orders: number
}

/** E 服务规模 */
export interface ScaleVO {
  onHandQty: number
  rackCount: number
  rackMonthlyFee: number
}

/** 看板总返回 */
export interface OperatorDashboardVO {
  overview: OverviewVO
  trend: TrendVO
  products: ProductStatVO[]
  ownerIncomeTop: OwnerStatVO[]
  ownerOutboundTop: OwnerOrderStatVO[]
  scale: ScaleVO
}
