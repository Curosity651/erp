export type AccountScope = 'OWNER_ACCOUNT' | 'PLATFORM_ACCOUNT'
export type RechargeStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'REVERSED'

export interface FundAccount {
  accountScope: AccountScope
  wmsTenantId: number
  wmsTenantName?: string
  erpTenantId?: number
  erpTenantName?: string
  currency: string
  rechargeAmount: number
  chargeAmount: number
  balance: number
  negative: boolean
  pendingCount: number
  lastChangeTime?: string
}

export interface RechargeOrder {
  id: number
  rechargeNo: string
  accountScope: AccountScope
  wmsTenantId: number
  wmsTenantName?: string
  erpTenantId?: number
  erpTenantName?: string
  amount: number
  currency: string
  paymentTime: string
  voucherFileId: number
  voucherUrl?: string
  status: RechargeStatus
  applicantId: number
  applicantName: string
  reviewerName?: string
  reviewTime?: string
  rejectReason?: string
  reverseReason?: string
  remark?: string
  createTime: string
}

export interface FundLedger {
  month?: string
  entryType: 'RECHARGE' | 'CHARGE' | 'REVERSAL'
  entryTypeLabel?: string
  businessNo: string
  businessLabel?: string
  description: string
  descriptionLabel?: string
  amount: number
  currency: string
  status: string
  voucherFileId?: number
  occurredTime: string
  operatorName?: string
}

export interface FundLedgerMonth {
  month: string
  rechargeAmount: number
  chargeAmount: number
  reversalAmount: number
  netChange: number
  openingBalance: number
  closingBalance: number
  currency: string
  statementStatus: 'REALTIME' | 'UNBILLED' | 'DRAFT' | 'CONFIRMED' | 'PAID'
  details: FundLedger[]
}

export interface RechargeCreateForm {
  amount: number
  currency: string
  paymentTime: string
  voucherFileId: number
  remark?: string
}

export interface FundQuery {
  erpTenantId?: number
  wmsTenantId?: number
  currency?: string
  status?: string
  negative?: boolean
  startDate?: string
  endDate?: string
}
