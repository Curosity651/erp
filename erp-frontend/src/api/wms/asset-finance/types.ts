import type { SkuBriefVO } from '@/api/common/sku-types'

/** 资产按币种（采购成本 + 物流附加；物流仅 USD 行有值） */
export interface AssetCurrencyVO {
  currency: string
  procurement: number
  logistics: number
  total: number
}

/** 应付按币种 */
export interface PayableCurrencyVO {
  currency: string
  contract: number
  paid: number
  outstanding: number
}

/** 资产与账务总览 */
export interface AssetFinanceOverviewVO {
  assets: AssetCurrencyVO[]
  supplierPayable: PayableCurrencyVO[]
  providerPayable: PayableCurrencyVO
}

/** 采购成本明细行（SKU × 币种） */
export interface AssetProcurementRowVO {
  skuCode: string
  skuBrief?: SkuBriefVO
  currency: string
  /** 归入该币种的持有量（A–E 全链路） */
  heldQty: number
  /** 加权平均采购单价 */
  avgUnitPrice: number
  /** 采购成本 = heldQty × avgUnitPrice */
  procurementCost: number
}

/** 物流附加成本明细行（SKU，USD） */
export interface AssetLogisticsRowVO {
  skuCode: string
  skuBrief?: SkuBriefVO
  /** 已发运在库量（A+B+C） */
  shippedHeldQty: number
  /** 物流单位成本 USD */
  unitCostUsd: number
  /** 物流附加成本 USD */
  logisticsCostUsd: number
}

/** 应付供应商下钻——采购单 */
export interface PayableSupplierOrderVO {
  orderNo: string
  orderStatus: string
  totalAmount: number
  prepayAmount: number
  balanceAmount: number
  prepayPaid: boolean
  balancePaid: boolean
  paid: number
  outstanding: number
  prepayTime: string | null
  balancePayTime: string | null
}

/** 应付供应商（按供应商 × 币种汇总 + 下钻） */
export interface PayableSupplierVO {
  supplierId: number
  supplierName: string
  currency: string
  contract: number
  paid: number
  outstanding: number
  orders: PayableSupplierOrderVO[]
}

/** 应付物流商下钻——物流单 */
export interface PayableProviderOrderVO {
  shippingNo: string
  shippingStatus: string
  totalAmount: number
  totalAmountCny: number
  paid: boolean
  outstanding: number
}

/** 应付物流商（按物流商汇总 + 下钻，USD） */
export interface PayableProviderVO {
  providerId: number
  providerName: string
  contract: number
  contractCny: number
  paid: number
  outstanding: number
  orders: PayableProviderOrderVO[]
}
