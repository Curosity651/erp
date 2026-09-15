import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  FulfillmentItem,
  FulfillmentOrder,
  FulfillmentBatchResult,
  FulfillmentDispatchResult,
  FulfillmentPickTask,
  FulfillmentPickTaskDetail,
  FulfillmentPickPackage,
  PlatformLabelResult,
  ManualFulfillmentForm,
  FulfillmentShelfOrderQuery,
  FulfillmentPickTaskQuery,
  OutboundPickReviewConfirmForm,
  OutboundPickReviewRecord,
  OutboundPickReviewSummary,
  FulfillmentShippingOrder,
  FulfillmentShippingQuery
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

export function listFulfillmentShelfOrders(params?: FulfillmentShelfOrderQuery) {
  return httpClient.get<ApiResult<FulfillmentOrder[]>>(`${pickingBaseUrl}/shelf-orders`, { params })
}

export function acceptFulfillmentOrders(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentDispatchResult>>(`${pickingBaseUrl}/accept`, ids)
}

export function redispatchFulfillmentOrders(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentDispatchResult>>(`${pickingBaseUrl}/redispatch`, ids)
}

export function createFulfillmentPickTask(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentPickTask>>(`${pickingBaseUrl}/tasks`, {
    fulfillmentOrderIds: ids
  })
}

export function listFulfillmentPickTasks(params?: FulfillmentPickTaskQuery) {
  return httpClient.get<ApiResult<FulfillmentPickTask[]>>(`${pickingBaseUrl}/tasks`, { params })
}

export function getFulfillmentPickTask(id: number, fulfillmentOrderId?: number) {
  return httpClient.get<ApiResult<FulfillmentPickTaskDetail>>(`${pickingBaseUrl}/tasks/${id}`, {
    params: { fulfillmentOrderId }
  })
}

export const claimFulfillmentPickTask = (id: number) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${id}/claim`)

export const releaseFulfillmentPickTask = (id: number) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${id}/release`)

export const transferFulfillmentPickTask = (id: number, operatorId: number) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${id}/transfer`, { operatorId })

export const markFulfillmentPickException = (
  taskId: number,
  orderId: number,
  dto: { exceptionType: string; reason: string; imageUrls?: string[] }
) =>
  httpClient.post<ApiResult<void>>(
    `${pickingBaseUrl}/tasks/${taskId}/orders/${orderId}/exception`,
    dto
  )

export const restoreFulfillmentPickException = (taskId: number, orderId: number) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${taskId}/orders/${orderId}/restore`)

export const cancelFulfillmentPickException = (taskId: number, orderId: number) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${taskId}/orders/${orderId}/cancel`)

export function scanFulfillmentPickLine(dto: {
  taskId: number
  fulfillmentNo: string
  locationCode: string
  warehouseSkuCode: string
  quantity: number
}) {
  return httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/scan`, dto)
}

export const completeSimplifiedFulfillmentTask = (taskId: number, evidenceFileIds: number[]) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${taskId}/simplified-complete`, {
    evidenceFileIds
  })

export const startSimplifiedFulfillmentTask = (taskId: number) =>
  httpClient.post<ApiResult<void>>(`${pickingBaseUrl}/tasks/${taskId}/simplified-start`)

const pickReviewBaseUrl = '/wms/outbound-pick-review'

export function getOutboundPickReviewSummary(params: {
  workDate: string
  warehouseId?: number
}) {
  return httpClient.get<ApiResult<OutboundPickReviewSummary>>(`${pickReviewBaseUrl}/summary`, {
    params
  })
}

export function generateFulfillmentPickPackage(id: number) {
  return httpClient.post<ApiResult<FulfillmentPickPackage>>(
    `${pickingBaseUrl}/tasks/${id}/print-package`
  )
}

export function confirmOutboundPickReview(dto: OutboundPickReviewConfirmForm) {
  return httpClient.post<ApiResult<OutboundPickReviewRecord>>(
    `${pickReviewBaseUrl}/confirm`,
    dto
  )
}

const shippingBaseUrl = '/wms/fulfillment-shipping'

export function listFulfillmentShippingOrders() {
  return httpClient.get<ApiResult<FulfillmentOrder[]>>(shippingBaseUrl)
}

export function pageFulfillmentShippingOrders(params: FulfillmentShippingQuery) {
  return httpClient.get<ApiResult<PageResult<FulfillmentShippingOrder>>>(
    `${shippingBaseUrl}/page`,
    {
      params
    }
  )
}

export function printFulfillmentLabel(id: number) {
  return httpClient.post<ApiResult<PlatformLabelResult>>(`${shippingBaseUrl}/${id}/label`)
}

export function verifyFulfillmentLabel(id: number, barcode: string) {
  return httpClient.post<ApiResult<void>>(`${shippingBaseUrl}/${id}/label/verify`, { barcode })
}

export function packFulfillment(
  id: number,
  dto: {
    carrierCode?: string
    carrierName: string
    shippingMethod: string
    trackingNo: string
    packageWeightKg: number
  }
) {
  return httpClient.post<ApiResult<void>>(`${shippingBaseUrl}/${id}/pack`, dto)
}

export function adjustFulfillmentLogisticsFee(
  id: number,
  dto: {
    amount: number
    adjustmentReason?: string
  }
) {
  return httpClient.post<ApiResult<void>>(`${shippingBaseUrl}/${id}/logistics-fee`, dto)
}

export function shipFulfillmentOrders(ids: number[]) {
  return httpClient.post<ApiResult<FulfillmentBatchResult>>(`${shippingBaseUrl}/ship`, ids)
}
