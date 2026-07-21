import type { SkuBriefVO } from '@/api/common/sku-types'
import type { OrderItemVO } from '@/api/order/types'

// ===================== 枚举类型 =====================

/**
 * 对账状态枚举
 * - PENDING: 待履约（订单尚未发货）
 * - IN_TRANSIT: 运输中（订单已发货未交付）
 * - CANCELED: 已取消（发货前取消，无需财务记录）
 * - MATCHED: 已对账（财务记录正常匹配）
 * - ANOMALY: 异常（应有财务记录但缺失或无销售记录）
 */
export type ReconciliationStatus =
  | 'PENDING' // 待履约
  | 'IN_TRANSIT' // 运输中
  | 'CANCELED' // 已取消
  | 'MATCHED' // 已对账
  | 'ANOMALY' // 异常

/**
 * 周期类型枚举
 */
export type PeriodType = 'weekly' | 'daily'

/**
 * 财务记录类型枚举
 */
export type FinancialRecordType =
  | 'SALE'
  | 'RETURN'
  | 'LOGISTICS'
  | 'STORAGE'
  | 'PENALTY'
  | 'COMPENSATION'
  | 'OTHER'

// ===================== 查询对象 =====================

/**
 * 订单对账查询参数
 */
export interface OrderReconciliationQO {
  /** 周期类型（必填）：weekly-周报 / daily-日报 */
  periodType: PeriodType
  /** 对账状态（多选） */
  reconciliationStatus?: ReconciliationStatus[]
  /** 店铺ID列表 */
  shopIds?: number[]
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 订单号/商品编码 */
  keyword?: string
}

// ===================== 统计视图对象 =====================

/**
 * 订单对账统计 VO
 */
export interface OrderReconciliationStatsVO {
  /** 订单总数 */
  totalOrders: number
  /** 待履约数量 */
  pendingCount: number
  /** 运输中数量 */
  inTransitCount: number
  /** 已取消数量 */
  canceledCount: number
  /** 已对账数量 */
  matchedCount: number
  /** 异常数量（应有财务记录但缺失） */
  anomalyCount: number
}

// ===================== 列表视图对象 =====================

/**
 * 订单对账列表 VO
 */
export interface OrderReconciliationVO {
  /** 订单ID */
  orderId: number
  /** 平台名称 */
  platform: string
  /** 平台订单号 */
  platformOrderId: string
  /** RID */
  rid: string
  /** 店铺ID */
  shopId: number
  /** 店铺名称 */
  shopName: string
  /** 履约类型 */
  fulfillmentType: string
  /** 商品编码 */
  article: string
  /** 订单数量 */
  quantity: number
  /** SKU简要信息 */
  skuBrief?: SkuBriefVO
  /** 订单商品明细 */
  items?: OrderItemVO[]
  /** ERP状态 */
  erpStatus: string
  /** 平台子状态（商家处理状态） */
  platformSubstatus: string
  /** 平台状态（履约状态） */
  platformStatus: string
  /** 对账状态 */
  reconciliationStatus: ReconciliationStatus
  /** 财务记录条数 */
  financialRecordCount: number
  /** 销售金额（订单金额） */
  saleAmount: number
  /** 实际收入（财务汇总） */
  actualIncome: number
  /** 财务记录币种 */
  financialCurrencyName?: string
  /** 订单时间（UTC） */
  orderTime: string
  /** 订单时间（莫斯科时区） */
  orderTimeMoscow: string
  /** 订单总金额（原始币种） */
  totalAmount?: number
  /** 货币代码 */
  currencyCode?: string
  /** 转换后金额（CNY） */
  convertedAmount?: number
  /** 转换后货币代码 */
  convertedCurrencyCode?: string
}

// ===================== 详情视图对象 =====================

/**
 * 财务记录项 VO
 */
export interface FinancialRecordItemVO {
  /** 记录ID */
  id: number
  /** 报表ID */
  realizationreportId: number
  /** 报表周期类型 */
  periodType: string
  /** 报表开始日期 */
  dateFrom: string
  /** 报表结束日期 */
  dateTo: string
  /** 文档类型名称 */
  docTypeName: string
  /** 供应商操作名称 */
  supplierOperName: string
  /** 奖惩类型名称 */
  bonusTypeName: string
  /** 数量 */
  quantity: number
  /** 应付金额 */
  ppvzForPay: number
  /** 罚款 */
  penalty: number
  /** 扣款 */
  deduction: number
  /** 币种名称 */
  currencyName: string
  /** 销售日期 */
  saleDt: string
  /** 报表日期 */
  rrDt: string
}

/**
 * 财务汇总信息 VO
 */
export interface FinancialSummaryVO {
  /** 记录总数 */
  recordCount: number
  /** 销售记录数 */
  saleRecordCount: number
  /** 退货记录数 */
  returnRecordCount: number
  /** 销售金额（正向销售 ppvz_for_pay 合计） */
  saleAmount: number
  /** 退货金额（退货类 ppvz_for_pay 合计，正数） */
  returnAmount: number
  /** 罚款金额合计 */
  penaltyAmount: number
  /** 扣款金额合计 */
  deductionAmount: number
  /** 实际收入（净额）= 销售 - 退货 - 罚款 - 扣款 */
  actualIncome: number
  /** 币种名称 */
  currencyName?: string
}

/**
 * 订单对账详情 VO
 */
export interface OrderReconciliationDetailVO {
  // ==================== 订单信息 ====================
  /** 订单ID */
  orderId: number
  /** 平台订单号 */
  platformOrderId: string
  /** 订单RID */
  rid: string
  /** 店铺ID */
  shopId: number
  /** 店铺名称 */
  shopName: string
  /** 商品编码 */
  article: string
  /** 商品数量 */
  quantity: number
  /** 履约类型 */
  fulfillmentType: string
  /** SKU简要信息 */
  skuBrief?: SkuBriefVO
  /** 订单商品明细 */
  items?: OrderItemVO[]

  // ==================== 状态信息 ====================
  /** ERP状态 */
  erpStatus: string
  /** 商家处理状态 */
  platformSubstatus: string
  /** 平台履约状态 */
  platformStatus: string
  /** 对账状态 */
  reconciliationStatus: ReconciliationStatus

  // ==================== 金额与时间 ====================
  /** 订单总金额（卢布） */
  totalAmountRub: number
  /** 订单创建时间（莫斯科时区） */
  orderTimeMoscow: string

  // ==================== 财务汇总 ====================
  /** 财务汇总信息 */
  financialSummary: FinancialSummaryVO

  // ==================== 关联财务记录 ====================
  /** 关联的财务记录列表 */
  financialRecords: FinancialRecordItemVO[]
}
