import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type { ReturnOrderVO, ReturnQO, ReturnReceiptDTO, ReturnProcessDTO } from './types'

const BASE = '/wms/return-qc'

export function pageReturns(pageParam: PageParam, qo: ReturnQO) {
  return httpClient.get<ApiResult<PageResult<ReturnOrderVO>>>(`${BASE}/page`, {
    params: { ...pageParam, ...qo }
  })
}

export function getReturnDetail(id: number) {
  return httpClient.get<ApiResult<ReturnOrderVO>>(`${BASE}/${id}`)
}

export function registerReturnReceipt(dto: ReturnReceiptDTO) {
  return httpClient.post<ApiResult<number[]>>(`${BASE}/receipts`, dto)
}

export function processReturnDisposition(dto: ReturnProcessDTO) {
  return httpClient.post<ApiResult<void>>(`${BASE}/process`, dto)
}

export function closeReturn(id: number) {
  return httpClient.post<ApiResult<void>>(`${BASE}/${id}/close`)
}

export function getAvailableLocations(returnOrderId: number, warehouseId: number, zone: string) {
  return httpClient.get<ApiResult<string[]>>(`${BASE}/available-locations`, {
    params: { returnOrderId, warehouseId, zone }
  })
}
