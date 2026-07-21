import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { PalletSlotVO } from '@/api/wms/inbound-execution'

export interface PalletItemVO {
  erpTenantId: number
  ownerName?: string
  skuCode: string
  quantity: number
  reservedQty: number
  quality: string
  inboundDate?: string
}

export interface PalletSummaryVO {
  id: number
  palletNo: string
  warehouseId: number
  warehouseName?: string
  slotCode?: string
  palletType: string
  palletStatus: string
  capacityPercent?: number
  capacitySource: string
  estimatedWeightKg?: number
  actualWeightKg?: number
  skuKindCount: number
  wholePalletEligible: number
  createTime: string
  items: PalletItemVO[]
}

export function listPallets(params?: {
  warehouseId?: number
  erpTenantId?: number
  skuCode?: string
  status?: string
}) {
  return httpClient.get<ApiResult<PalletSummaryVO[]>>('/wms/pallets', { params })
}

export function getPallet(id: number) {
  return httpClient.get<ApiResult<PalletSummaryVO>>(`/wms/pallets/${id}`)
}

export function listPalletSlots(warehouseId: number) {
  return httpClient.get<ApiResult<PalletSlotVO[]>>('/wms/pallets/slots', { params: { warehouseId } })
}

export function calibratePallet(data: {
  palletId: number
  capacityPercent: number
  actualWeightKg?: number
  markFull?: boolean
  remark?: string
}) {
  return httpClient.patch<ApiResult<unknown>>('/wms/pallets/capacity', data)
}
