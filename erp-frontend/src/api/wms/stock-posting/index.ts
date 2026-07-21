import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  StockPostingPageParam,
  StockPostingPageVO,
  StockPostingDetailVO,
  StockPostingItemPageParam,
  StockPostingItemPageVO
} from './types'

/**
 * 过账单分页查询
 */
export function pageStockPosting(params: StockPostingPageParam) {
  return httpClient.get<ApiResult<PageResult<StockPostingPageVO>>>('/wms/stock-posting/page', {
    params
  })
}

/**
 * 获取过账单详情（含明细）
 */
export function getStockPostingDetail(id: number) {
  return httpClient.get<ApiResult<StockPostingDetailVO>>('/wms/stock-posting/detail', {
    params: { id }
  })
}

/**
 * 过账单明细分页查询
 */
export function pageStockPostingItem(params: StockPostingItemPageParam) {
  return httpClient.get<ApiResult<PageResult<StockPostingItemPageVO>>>(
    '/wms/stock-posting/item/page',
    {
      params
    }
  )
}
