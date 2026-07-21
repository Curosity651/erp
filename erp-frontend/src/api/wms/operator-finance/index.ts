import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type { ExpenseBillVO, IncomeRecord, IncomeSummary } from './types'

const BASE = '/wms/operator-finance'

/** 收入汇总（按产品×月：次数×单价） */
export function getIncomeSummary(params: {
  monthStart?: string
  monthEnd?: string
  erpTenantId?: number
}) {
  return httpClient.get<ApiResult<IncomeSummary>>(`${BASE}/income/summary`, { params })
}

/** 收入明细流水 */
export function listIncomeRecords(params: {
  monthStart?: string
  monthEnd?: string
  erpTenantId?: number
  productId?: number
}) {
  return httpClient.get<ApiResult<IncomeRecord[]>>(`${BASE}/income/records`, { params })
}

/** 支出账单分页（只读对账） */
export function pageExpenseBills(
  pageParam: PageParam,
  qo: { billMonthStart?: string; billMonthEnd?: string; statuses?: string[] }
) {
  return httpClient.get<ApiResult<PageResult<ExpenseBillVO>>>(`${BASE}/expense/page`, {
    params: { ...pageParam, ...qo }
  })
}

/** 支出账单详情 */
export function getExpenseBillDetail(id: number) {
  return httpClient.get<ApiResult<ExpenseBillVO>>(`${BASE}/expense/${id}`)
}
