import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  FulfillmentItem,
  FulfillmentOrder,
  FulfillmentBatchResult,
  FulfillmentPickTask,
  FulfillmentPickTaskDetail,
  PlatformLabelResult,
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

const pickingBaseUrl = '/wms/fulfillment-picking'

export function listFulfillmentShelfOrders() {
  return httpClient.get<ApiResult<FulfillmentOrder[]>>(`${pickingBaseUrl}/shelf-orders`)
}

export function acceptFulfillmentOrders(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentBatchResult>>(`${pickingBaseUrl}/accept`, ids)
}

export function createFulfillmentPickTask(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentPickTask>>(`${pickingBaseUrl}/tasks`, {
    fulfillmentOrderIds: ids
  })
}

export function listFulfillmentPickTasks() {
  return httpClient.get<ApiResult<FulfillmentPickTask[]>>(`${pickingBaseUrl}/tasks`)
}

export function getFulfillmentPickTask(id: number) {
  return httpClient.get<ApiResult<FulfillmentPickTaskDetail>>(`${pickingBaseUrl}/tasks/${id}`)
}

export function scanFulfillmentPickLine(dto: {
  taskId: number
  fulfillmentNo: string
  locationCode: string
  warehouseSkuCode: string
  quantity: number
}) {
  return httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/scan`, dto)
}

const shippingBaseUrl = '/wms/fulfillment-shipping'

export function listFulfillmentShippingOrders() {
  return httpClient.get<ApiResult<FulfillmentOrder[]>>(shippingBaseUrl)
}

export function printFulfillmentLabel(id: number) {
  return httpClient.post<ApiResult<PlatformLabelResult>>(`${shippingBaseUrl}/${id}/label`)
}

export function verifyFulfillmentLabel(id: number, barcode: string) {
  return httpClient.post<ApiResult<void>>(`${shippingBaseUrl}/${id}/label/verify`, { barcode })
}

export function packFulfillment(id: number, dto: {
  carrierCode?: string
  carrierName: string
  shippingMethod: string
  trackingNo: string
  packageWeightKg: number
}) {
  return httpClient.post<ApiResult<void>>(`${shippingBaseUrl}/${id}/pack`, dto)
}

export function adjustFulfillmentLogisticsFee(id: number, dto: {
  amount: number
  adjustmentReason?: string
}) {
  return httpClient.post<ApiResult<void>>(`${shippingBaseUrl}/${id}/logistics-fee`, dto)
}

export function shipFulfillmentOrders(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentBatchResult>>(`${shippingBaseUrl}/ship`, ids)
}
