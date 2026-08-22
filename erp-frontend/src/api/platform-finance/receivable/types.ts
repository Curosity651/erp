export type BillStatus = 'DRAFT' | 'CONFIRMED' | 'PAID' | 'DISPUTED'

export interface BillingRecordVO {
  id: number
  erpTenantId?: number
  ownerName?: string
  warehouseId?: number
  feeType: string
  feeCode: string
  feeName: string
  billingUnit: string
  billingQuantity: number
  unitPrice: number
  amount: number
  currency: string
  sourceType?: string
  sourceRef?: string
  remark?: string
  createTime?: string
}

export interface MonthlyBillVO {
  id: number
  billMonth: string
  wmsTenantId: number
  wmsTenantName: string
  rackFee: number
  inboundFee: number
  outboundFee: number
  deliveryFee: number
  returnFee: number
  inspectionFee: number
  driverFee: number
  totalAmount: number
  status: BillStatus
  confirmedTime?: string
  paidTime?: string
  paymentVoucherFileId?: number
  remark?: string
  billingRecords?: BillingRecordVO[]
}

export interface MonthlyBillQO {
  billMonthStart?: string
  billMonthEnd?: string
  wmsTenantId?: number
  statuses?: BillStatus[]
}

export interface GenerateBillDTO {
  billMonth: string
  wmsTenantId?: number
}

export interface GenerateBillResultVO {
  created: number
  recalculated: number
  skipped: number
}

export interface FeeRate {
  id: number
  wmsTenantId: number
  feeCode: string
  feeName: string
  feeType: string
  billingUnit: string
  unitPrice: number
  currency: string
  remark?: string
}

export interface ManualBillingDTO {
  wmsTenantId: number
  erpTenantId?: number
  warehouseId?: number
  billMonth: string
  feeCode: string
  quantity: number
  actualAmount?: number
  sourceRef: string
  remark: string
}
