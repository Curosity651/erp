export type LedgerStatementStatus = 'REALTIME' | 'UNBILLED' | 'DRAFT' | 'CONFIRMED' | 'PAID'

const statusMeta: Record<LedgerStatementStatus, { text: string; color: string }> = {
  REALTIME: { text: '实时统计', color: 'blue' },
  UNBILLED: { text: '未生成对账单', color: 'default' },
  DRAFT: { text: '待复核', color: 'orange' },
  CONFIRMED: { text: '已复核', color: 'green' },
  PAID: { text: '已付款', color: 'cyan' }
}

export function ledgerStatementMeta(status: LedgerStatementStatus) {
  return statusMeta[status] || statusMeta.UNBILLED
}
