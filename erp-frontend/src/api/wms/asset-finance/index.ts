import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  AssetFinanceOverviewVO,
  AssetProcurementRowVO,
  AssetLogisticsRowVO,
  PayableSupplierVO,
  PayableProviderVO
} from './types'

/** 资产与账务总览 */
export function getAssetFinanceOverview() {
  return httpClient.get<ApiResult<AssetFinanceOverviewVO>>('/wms/asset-finance/overview')
}

/** 采购成本明细（SKU × 币种） */
export function getProcurementDetail() {
  return httpClient.get<ApiResult<AssetProcurementRowVO[]>>('/wms/asset-finance/procurement-detail')
}

/** 物流附加成本明细（SKU，USD） */
export function getLogisticsDetail() {
  return httpClient.get<ApiResult<AssetLogisticsRowVO[]>>('/wms/asset-finance/logistics-detail')
}

/** 应付供应商（汇总 + 下钻采购单） */
export function getPayableSupplier() {
  return httpClient.get<ApiResult<PayableSupplierVO[]>>('/wms/asset-finance/payable-supplier')
}

/** 应付物流商（汇总 + 下钻物流单） */
export function getPayableProvider() {
  return httpClient.get<ApiResult<PayableProviderVO[]>>('/wms/asset-finance/payable-provider')
}
