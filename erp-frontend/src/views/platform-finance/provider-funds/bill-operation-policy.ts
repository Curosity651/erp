import type { BillStatus } from '@/api/platform-finance/receivable/types'

export function billOperations(status: BillStatus) {
  const editable = status === 'DRAFT' || status === 'DISPUTED'
  return { view: true, adjust: editable, review: editable }
}
