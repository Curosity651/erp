import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { OperatorDashboardParams, OperatorDashboardVO } from './types'

/** 服务商运营看板聚合数据（月区间默认最近6个月） */
export function getOperatorDashboardData(params: OperatorDashboardParams) {
  return httpClient.get<ApiResult<OperatorDashboardVO>>('/wms/operator-dashboard/data', {
    params
  })
}
