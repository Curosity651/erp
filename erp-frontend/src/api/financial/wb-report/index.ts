import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  WbReportDetailPageParam,
  WbReportDetailVO,
  WbReportDetailQO,
  FullSyncRequest,
  SyncTaskResponse
} from './types'

/**
 * 分页查询财务报表明细
 * @param pageParams 分页参数和查询条件
 */
export function pageWbReportDetail(pageParams: WbReportDetailPageParam) {
  return httpClient.get<ApiResult<PageResult<WbReportDetailVO>>>('/financial/wb-report/page', {
    params: pageParams
  })
}

/**
 * 导出财务报表明细
 * @param qo 查询条件
 */
export function exportWbReportDetail(qo: WbReportDetailQO) {
  return httpClient.post('/financial/wb-report/export', qo, {
    responseType: 'blob'
  })
}

/**
 * 按时间范围同步财务报表（统一入口）
 */
export function syncWbReport(request: FullSyncRequest) {
  return httpClient.post<ApiResult<SyncTaskResponse>>('/financial/wb-report/sync', request)
}
