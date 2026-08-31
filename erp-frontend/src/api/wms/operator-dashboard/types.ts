export interface OperatorDashboardParams {
  monthStart?: string
  monthEnd?: string
  erpTenantIds?: number[]
}

export interface CurrencyAmountVO {
  currency: string
  amount: number
}

export interface ExpenseSummaryVO {
  currency: string
  draftEstimate: number
  confirmedPayable: number
  disputedAmount: number
  paidAmount: number
}

export interface CurrencySeriesVO {
  currency: string
  amounts: number[]
}

export interface OverviewVO {
  incomeByCurrency: CurrencyAmountVO[]
  incomeCount: number
  expenseByCurrency: ExpenseSummaryVO[]
  balanceByCurrency: CurrencyAmountVO[]
  ownerTotal: number
  ownerEnabled: number
  productTotal: number
  productEnabled: number
}

export interface TrendVO {
  months: string[]
  incomeSeries: CurrencySeriesVO[]
  confirmedExpenseSeries: CurrencySeriesVO[]
  paidExpenseSeries: CurrencySeriesVO[]
}

export interface ProductStatVO {
  productId: number
  productName: string
  currency: string
  usageCount: number
  amount: number
}

export interface OwnerStatVO {
  erpTenantId: number
  ownerName: string
  currency: string
  amount: number
  usageCount: number
}

export interface OwnerOrderStatVO {
  erpTenantId: number
  ownerName: string
  orders: number
}

export interface ScaleVO {
  onHandQty: number
  rackCount: number
  rackMonthlyFee: number
  rackMonthlyFeeCurrency: string
}

export interface OperatorDashboardVO {
  overview: OverviewVO
  trend: TrendVO
  products: ProductStatVO[]
  ownerIncomeTop: OwnerStatVO[]
  ownerOutboundTop: OwnerOrderStatVO[]
  scale: ScaleVO
}
