import httpClient from '@/utils/axios'
import type { ApiResult, PageResult, PageParam } from '@/api/types'
import type {
  GlobalInventoryConfigVO,
  GlobalInventoryConfigDTO,
  InventoryConfigVO,
  InventoryConfigDTO,
  InventoryConfigQO,
  ForecastSummaryResult,
  ForecastSummaryQO,
  ForecastDetailVO,
  ForecastDetailQO,
  IncomingPlanVO
} from './types'

// ========== 全局配置 ==========

export function getGlobalConfig() {
  return httpClient.get<ApiResult<GlobalInventoryConfigVO>>('/wms/inventory-config/global')
}

export function saveGlobalConfig(dto: GlobalInventoryConfigDTO) {
  return httpClient.put<ApiResult<void>>('/wms/inventory-config/global', dto)
}

// ========== SKU 独立配置 ==========

export function pageSkuConfig(params: PageParam & InventoryConfigQO) {
  return httpClient.get<ApiResult<PageResult<InventoryConfigVO>>>('/wms/inventory-config/sku', {
    params
  })
}

export function saveSkuConfig(dto: InventoryConfigDTO) {
  return httpClient.put<ApiResult<void>>('/wms/inventory-config/sku', dto)
}

export function deleteSkuConfig(id: number) {
  return httpClient.delete<ApiResult<void>>('/wms/inventory-config/sku', {
    params: { id }
  })
}

// ========== 库存预测 ==========

export function getForecastSummary(params: PageParam & ForecastSummaryQO) {
  return httpClient.get<ApiResult<ForecastSummaryResult>>('/wms/inventory-forecast/summary', {
    params
  })
}

export function getForecastDetail(params: ForecastDetailQO) {
  return httpClient.get<ApiResult<ForecastDetailVO>>('/wms/inventory-forecast/detail', {
    params
  })
}

export function getIncomingPlan(warehouseId: number, skuCode: string) {
  return httpClient.get<ApiResult<IncomingPlanVO[]>>('/wms/inventory-forecast/incoming', {
    params: { warehouseId, skuCode }
  })
}
