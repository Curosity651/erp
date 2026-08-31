/**
 * WMS 服务商财务 类型契约。
 * 收入（链路二）：名下货主使用物流产品，按次×单价计费。
 * 支出（链路一）：服务商应付平台的月度账单，只读对账（平台不介入管理）。
 */

/** 收入汇总行（产品×月） */
export interface IncomeSummaryRow {
  productId: number
  productName: string
  tags: string[]
  unitPrice?: number
  currency: string
  billMonth: string
  usageCount: number
  subtotal: number
}

/** 收入汇总 */
export interface IncomeSummary {
  currencyTotals: Array<{ currency: string; amount: number }>
  totalCount: number
  rows: IncomeSummaryRow[]
}

/** 收入明细流水 */
export interface IncomeRecord {
  id: number
  billMonth: string
  productId: number
  productName: string
  productNameSnapshot?: string
  productDescriptionSnapshot?: string
  ownerName?: string
  businessNo?: string
  platformOrderId?: string
  trackingNo?: string
  amount: number
  currency: string
  createTime: string
}

/** 支出账单（对齐 wms_monthly_bill，服务商侧只读） */
export interface ExpenseBillVO {
  id: number
  billMonth: string
  wmsTenantId: number
  wmsTenantName?: string
  rackFee: number
  inboundFee: number
  outboundFee: number
  deliveryFee: number
  returnFee: number
  inspectionFee: number
  driverFee: number
  totalAmount: number
  currency: string
  status: 'DRAFT' | 'CONFIRMED' | 'PAID' | 'DISPUTED'
  confirmedTime?: string
  paidTime?: string
  remark?: string
}

/** 支出账单状态元数据 */
export const EXPENSE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿估算', color: 'default' },
  CONFIRMED: { label: '已确认应付', color: 'processing' },
  PAID: { label: '已付款', color: 'success' },
  DISPUTED: { label: '争议中', color: 'error' }
}
