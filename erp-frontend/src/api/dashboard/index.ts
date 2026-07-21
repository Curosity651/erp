import { rawAxios } from '@/utils/axios'
import httpClient from '@/utils/axios'
import type { AxiosResponse } from 'axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  DashboardQueryParams,
  DashboardDataVO,
  SkuRankingQueryParams,
  SkuRankingPageParams,
  SkuRankingItemVO
} from './types'

/**
 * 获取Dashboard数据（返回完整响应以便获取headers）
 */
export function getDashboardData(
  params: DashboardQueryParams
): Promise<AxiosResponse<ApiResult<DashboardDataVO>>> {
  return rawAxios.post<ApiResult<DashboardDataVO>>('/api/dashboard/data', params)
}

/**
 * 分页查询SKU销量排名
 */
export function getSkuRankingPage(
  pageParams: SkuRankingPageParams,
  queryParams: SkuRankingQueryParams
): Promise<PageResult<SkuRankingItemVO>> {
  return httpClient.post('/api/dashboard/sku-ranking/page', queryParams, {
    params: pageParams
  })
}

/**
 * 导出SKU销量排名
 */
export function exportSkuRanking(queryParams: SkuRankingQueryParams): Promise<AxiosResponse> {
  return rawAxios.post('/api/dashboard/sku-ranking/export', queryParams, {
    responseType: 'blob'
  })
}
