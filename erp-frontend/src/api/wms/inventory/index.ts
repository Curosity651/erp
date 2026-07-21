import httpClient from '@/utils/axios'
import type { ApiResult, PageResult, PageParam } from '@/api/types'
import type {
  InventoryPageParam,
  InventoryPageVO,
  InventoryDetailVO,
  InventorySummaryVO,
  WarehouseSummaryVO,
  SkuSummaryVO,
  RecentFlowVO,
  WarehouseSummaryQO,
  SkuSummaryQO,
  RegionSummaryVO,
  RegionInventoryStatsVO
} from './types'

/**
 * 库存汇总统计
 */
export function getInventorySummary() {
  return httpClient.get<ApiResult<InventorySummaryVO>>('/wms/inventory/summary')
}

/**
 * 按仓库汇总
 * @param params 查询条件
 */
export function getInventorySummaryByWarehouse(params?: WarehouseSummaryQO) {
  return httpClient.get<ApiResult<WarehouseSummaryVO[]>>('/wms/inventory/summary-by-warehouse', {
    params
  })
}

/**
 * 按SKU汇总分页
 * @param params 分页参数
 */
export function pageInventorySummaryBySku(params: PageParam & SkuSummaryQO) {
  return httpClient.get<ApiResult<PageResult<SkuSummaryVO>>>('/wms/inventory/summary-by-sku/page', {
    params
  })
}

/**
 * 库存明细分页查询
 * @param params 分页参数
 */
export function pageInventory(params: InventoryPageParam) {
  return httpClient.get<ApiResult<PageResult<InventoryPageVO>>>('/wms/inventory/page', {
    params
  })
}

/**
 * 获取库存明细详情
 * @param warehouseId 仓库ID
 * @param skuCode SKU编码
 */
export function getInventoryDetail(warehouseId: number, skuCode: string) {
  return httpClient.get<ApiResult<InventoryDetailVO>>('/wms/inventory/detail', {
    params: { warehouseId, skuCode }
  })
}

/**
 * 获取最近流水
 * @param params 查询参数
 */
export function getRecentFlows(params: { warehouseId: number; skuCode: string; limit?: number }) {
  return httpClient.get<ApiResult<RecentFlowVO[]>>('/wms/inventory/recent-flows', { params })
}

/**
 * 按区域汇总
 */
export function getInventorySummaryByRegion() {
  return httpClient.get<ApiResult<RegionSummaryVO[]>>('/wms/inventory/summary-by-region')
}

/**
 * 区域库存统计卡片
 */
export function getRegionInventoryStats() {
  return httpClient.get<ApiResult<RegionInventoryStatsVO>>('/wms/inventory/region-stats')
}
