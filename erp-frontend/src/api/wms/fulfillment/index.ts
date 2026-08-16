import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  FulfillmentItem,
  FulfillmentOrder,
  ManualFulfillmentForm
} from './types'

const baseUrl = '/wms/manual-fulfillment'

export function listManualFulfillment() {
  return httpClient.get<ApiResult<FulfillmentOrder[]>>(baseUrl)
}

export function getManualFulfillment(id: number) {
  return httpClient.get<ApiResult<FulfillmentOrder>>(`${baseUrl}/${id}`)
}

export function listManualFulfillmentItems(id: number) {
  return httpClient.get<ApiResult<FulfillmentItem[]>>(`${baseUrl}/${id}/items`)
}

export function createManualFulfillment(dto: ManualFulfillmentForm) {
  return httpClient.post<ApiResult<number>>(baseUrl, dto)
}

export function updateManualFulfillment(id: number, dto: ManualFulfillmentForm) {
  return httpClient.put<ApiResult<number>>(`${baseUrl}/${id}`, dto)
}

export function submitManualFulfillment(id: number) {
  return httpClient.post<ApiResult<void>>(`${baseUrl}/${id}/submit`)
}

export function deleteManualFulfillment(id: number) {
  return httpClient.delete<ApiResult<void>>(`${baseUrl}/${id}`)
}

