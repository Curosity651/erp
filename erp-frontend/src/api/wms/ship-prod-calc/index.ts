import httpClient from '@/utils/axios'
import type { ApiResult, PageParam } from '@/api/types'
import type { ShipProdCalcSummaryVO, ShipProdCalcDetailVO, ShipProdCalcQO } from './types'

/** 发货生产测算汇总（分页 + 状态卡计数） */
export function getShipProdSummary(params: PageParam & ShipProdCalcQO) {
  return httpClient.get<ApiResult<ShipProdCalcSummaryVO>>('/wms/ship-prod-calc/summary', { params })
}

/** 单 SKU 测算详情（数据链） */
export function getShipProdDetail(skuCode: string, baseDate?: string) {
  return httpClient.get<ApiResult<ShipProdCalcDetailVO>>('/wms/ship-prod-calc/detail', {
    params: { skuCode, baseDate }
  })
}
