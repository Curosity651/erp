import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  FboSyncResultVO,
  FboSyncLogPageParam,
  FboSyncLogPageVO,
  FboSyncLogDetailVO,
  FboInventoryPageParam,
  FboInventoryPageVO,
  FboInventorySummaryVO
} from './types'

export function getFboInventorySummary() {
  return httpClient.get<ApiResult<FboInventorySummaryVO>>('/wms/fbo/summary')
}

export function getFboInventoryPage(params: FboInventoryPageParam) {
  return httpClient.get<ApiResult<PageResult<FboInventoryPageVO>>>('/wms/fbo/page', { params })
}

/** 触发 FBO 库存同步 */
export function syncFboStock(shopId?: number) {
  return httpClient.post<ApiResult<FboSyncResultVO[]>>('/wms/fbo/sync', null, {
    params: shopId ? { shopId } : {}
  })
}

/** 查询同步日志分页 */
export function getFboSyncLogPage(params: FboSyncLogPageParam) {
  return httpClient.get<ApiResult<PageResult<FboSyncLogPageVO>>>('/wms/fbo/sync-log', { params })
}

/** 查询同步日志详情 */
export function getFboSyncLogDetail(id: number) {
  return httpClient.get<ApiResult<FboSyncLogDetailVO>>('/wms/fbo/sync-log/detail', {
    params: { id }
  })
}
