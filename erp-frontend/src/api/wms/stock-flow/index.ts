import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  StockFlowPageParam,
  StockFlowPageVO,
  StockFlowDetailVO,
  StockFlowTodaySummaryVO,
  StockFlowTrendVO
} from './types'

/**
 * 库存流水分页查询
 * @param params 分页参数
 */
export function pageStockFlow(params: StockFlowPageParam) {
  return httpClient.get<ApiResult<PageResult<StockFlowPageVO>>>('/wms/stock-flow/page', {
    params
  })
}

/**
 * 获取流水详情
 * @param id 流水ID
 */
export function getStockFlowDetail(id: number) {
  return httpClient.get<ApiResult<StockFlowDetailVO>>('/wms/stock-flow/detail', { params: { id } })
}

/**
 * 查询该仓+SKU 实际出现过的过账类型（用于筛选下拉动态选项）
 * @param warehouseId 仓库ID
 * @param skuCode SKU编码
 */
export function listStockFlowPostingTypes(warehouseId?: number, skuCode?: string) {
  return httpClient.get<ApiResult<string[]>>('/wms/stock-flow/posting-types', {
    params: { warehouseId, skuCode }
  })
}

/**
 * 获取今日汇总统计
 * @param warehouseId 仓库ID（可选）
 */
export function getStockFlowTodaySummary(warehouseId?: number) {
  return httpClient.get<ApiResult<StockFlowTodaySummaryVO>>('/wms/stock-flow/today-summary', {
    params: { warehouseId }
  })
}

/**
 * 获取流水趋势数据
 * @param days 天数
 * @param warehouseId 仓库ID（可选）
 */
export function getStockFlowTrend(days = 7, warehouseId?: number) {
  return httpClient.get<ApiResult<StockFlowTrendVO[]>>('/wms/stock-flow/trend', {
    params: { days, warehouseId }
  })
}
