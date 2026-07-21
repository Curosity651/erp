import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type { LogisticsProductDTO, LogisticsProductVO } from './types'

const BASE = '/wms/logistics-product'

/** 产品分页（服务商） */
export function pageLogisticsProducts(pageParam: PageParam, keyword?: string, status?: number) {
  return httpClient.get<ApiResult<PageResult<LogisticsProductVO>>>(`${BASE}/page`, {
    params: { ...pageParam, keyword, status }
  })
}

/** 新建/编辑产品（服务商） */
export function saveLogisticsProduct(dto: LogisticsProductDTO) {
  return httpClient.post<ApiResult<void>>(BASE, dto)
}

/** 启用/停用产品（服务商） */
export function updateLogisticsProductStatus(id: number, status: number) {
  return httpClient.put<ApiResult<void>>(`${BASE}/${id}/status`, undefined, {
    params: { status }
  })
}

/** 删除产品（服务商） */
export function deleteLogisticsProduct(id: number) {
  return httpClient.delete<ApiResult<void>>(`${BASE}/${id}`)
}

/** 货主可选产品（父服务商启用中，建单选择器用） */
export function listOwnerLogisticsProducts() {
  return httpClient.get<ApiResult<LogisticsProductVO[]>>(`${BASE}/options`)
}
