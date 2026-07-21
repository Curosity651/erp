import type { BillStatus, MonthlyBillVO } from '@/api/platform-finance/receivable/types'

/**
 * 结算币种符号。
 * 假设：平台↔WMS服务商 按 ₽(RUB) 结算，与系统主币种一致。
 * 若实际按 ¥(CNY)，改这一个常量即可（后端接入时确认）。
 */
export const CURRENCY_SYMBOL = '₽'

/** 状态文案 */
export const BILL_STATUS_TEXT: Record<BillStatus, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  PAID: '已付款',
  DISPUTED: '争议'
}

/** 状态 tag 颜色 */
export const BILL_STATUS_COLOR: Record<BillStatus, string> = {
  DRAFT: 'default',
  CONFIRMED: 'processing',
  PAID: 'success',
  DISPUTED: 'error'
}

/** 状态筛选下拉项 */
export const BILL_STATUS_OPTIONS = (Object.keys(BILL_STATUS_TEXT) as BillStatus[]).map(v => ({
  label: BILL_STATUS_TEXT[v],
  value: v
}))

/** 费用科目元数据：货架租金 + 6 项操作费 */
export const FEE_FIELDS: { key: keyof MonthlyBillVO; label: string; group: 'rack' | 'op' }[] = [
  { key: 'rackFee', label: '货架租金', group: 'rack' },
  { key: 'inboundFee', label: '入库费', group: 'op' },
  { key: 'outboundFee', label: '出库费', group: 'op' },
  { key: 'deliveryFee', label: '配送费', group: 'op' },
  { key: 'returnFee', label: '退货费', group: 'op' },
  { key: 'inspectionFee', label: '验货费', group: 'op' },
  { key: 'driverFee', label: '司机费', group: 'op' }
]

/** 操作费小计（6 项之和） */
export function operationSubtotal(bill: MonthlyBillVO): number {
  return FEE_FIELDS.filter(f => f.group === 'op').reduce(
    (s, f) => s + (Number(bill[f.key]) || 0),
    0
  )
}

/** 金额格式化 */
export function formatMoney(v?: number | null): string {
  if (v == null) return '-'
  return `${CURRENCY_SYMBOL} ${v.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}
