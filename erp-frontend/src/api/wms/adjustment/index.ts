import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  ScrapCreateDTO,
  AdjustmentPageParam,
  AdjustmentPageVO,
  AdjustmentDetailVO,
  PhysicalBatchVO
} from './types'

/** 报废单分页 */
export function pageAdjustment(params: AdjustmentPageParam) {
  return httpClient.get<ApiResult<PageResult<AdjustmentPageVO>>>('/wms/adjustment/page', { params })
}

/** 报废单详情 */
export function getAdjustmentDetail(id: number) {
  return httpClient.get<ApiResult<AdjustmentDetailVO>>('/wms/adjustment/detail', { params: { id } })
}

/** 平台发起报废 */
export function createScrap(dto: ScrapCreateDTO) {
  return httpClient.post<ApiResult<number>>('/wms/adjustment', dto)
}

/** 平台撤销报废（待货主确认时） */
export function cancelScrap(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/adjustment/cancel', null, { params: { id } })
}

/** 货主确认销毁 */
export function ownerConfirmScrap(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/adjustment/owner-confirm', null, { params: { id } })
}

/** 货主驳回报废 */
export function ownerRejectScrap(id: number, reason?: string) {
  return httpClient.patch<ApiResult<void>>('/wms/adjustment/owner-reject', null, {
    params: { id, reason }
  })
}

/** 删除报废单（已取消/已驳回） */
export function deleteAdjustment(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/adjustment', { data: ids })
}

/** 某货主在某仓的批次（报废/移库选源用） */
export function listOwnerBatches(erpTenantId: number, warehouseId?: number, skuKeyword?: string) {
  return httpClient.get<ApiResult<PhysicalBatchVO[]>>('/wms/physical-inventory/batches', {
    params: { erpTenantId, warehouseId, skuKeyword }
  })
}
