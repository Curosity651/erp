import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  PurchaseInboundPageParam,
  PurchaseInboundPageVO,
  PurchaseInboundDetailVO
} from '@/api/wms/purchase-inbound/types'
import type { PalletSummaryVO } from '@/api/wms/pallet'

/** 收货明细项 */
export interface ReceiveItem {
  inboundOrderItemId: number
  skuCode: string
  actualQuantity: number
}

/** 收货 DTO */
export interface InboundReceiveDTO {
  inboundOrderId: number
  items: ReceiveItem[]
  evidenceFileIds: number[]
}

export interface PutawayRecordLine {
  skuCode: string
  locationId: number
  quantity: number
  quality: 'GOOD' | 'DAMAGED'
  capacityOverrideReason?: string
}

export interface PutawayRecordDTO {
  inboundOrderId: number
  lines: PutawayRecordLine[]
  confirmedVolumeCbm?: number
  afterHours?: boolean
  afterHoursReason?: string
}

/** 可用库位（上架分配用，空闲且分区匹配品质） */
export interface AvailableLocationVO {
  locationId: number
  locationCode: string
  zoneId: number
  zoneName?: string
  zoneType?: string
  rackNo?: string
  columnNo?: number
}

export interface PalletSlotVO {
  slotId: number
  locationId: number
  locationCode: string
  slotCode: string
  rackNo?: string
  columnNo?: number
  levelNo: number
  positionNo?: number
  zoneId?: number
  zoneName?: string
  zoneType?: string
  slotStatus: string
  maxWeightKg?: number
  palletId?: number
  palletNo?: string
  palletType?: string
  palletStatus?: string
  capacityPercent?: number
  skuKindCount?: number
}

export interface PalletPlanItem {
  erpTenantId: number
  skuCode: string
  skuName?: string
  quantity: number
  quantityPerPallet?: number
}

export interface PalletPlan {
  palletKey: string
  palletId?: number
  palletNo?: string
  existingPallet: boolean
  palletType: 'SINGLE_FULL' | 'SINGLE_PARTIAL' | 'MIXED'
  quality: 'GOOD' | 'DAMAGED'
  slotCode: string
  locationCode: string
  levelNo: number
  capacityPercent?: number
  capacitySource: string
  estimatedWeightKg?: number
  actualWeightKg?: number
  wholePalletEligible: boolean
  manualFull?: boolean
  items: PalletPlanItem[]
}

export interface InboundPutawayPlanVO {
  inboundOrderId: number
  warehouseId: number
  pallets: PalletPlan[]
  slotCandidates: PalletSlotVO[]
  warnings: string[]
  calculatedVolumeCbm?: number
  volumeMissingSkuCodes: string[]
}

export interface PutawayReceiptLineVO {
  palletId?: number
  locationId?: number
  locationCode?: string
  palletNo?: string
  slotCode?: string
  skuCode: string
  erpTenantId?: number
  warehouseSkuCode?: string
  quality: string
  quantity: number
  overrideReason?: string
}

export interface PutawayRecordLocationVO {
  locationId: number
  locationCode: string
  rackNo?: string
  zoneId?: number
  zoneName?: string
  zoneType?: string
  publicShared?: number
  capacityCalculable: boolean
  capacityVolumeMm3?: number
  occupiedVolumeMm3?: number
  occupiedWeightGrams?: number
  maxWeightGrams?: number
  skuKindCount?: number
  maxSkuKinds?: number
  volumeAllowed?: boolean
  weightAllowed?: boolean
  skuKindsAllowed?: boolean
  utilizationPercent?: number
}

export interface PutawayRecordSkuVO {
  skuCode: string
  warehouseSkuCode?: string
  skuName?: string
  imageUrl?: string
  receivedQuantity: number
  outerLengthMm?: number
  outerWidthMm?: number
  outerHeightMm?: number
  outerGrossWeightG?: number
}

export interface PutawayRecordContextVO {
  inboundOrderId: number
  inboundNo: string
  warehouseId: number
  erpTenantId: number
  ownerName?: string
  items: PutawayRecordSkuVO[]
  locations: PutawayRecordLocationVO[]
}

/** 平台待作业入库单分页（采购 + 自定义全来源，平台看全部） */
export function pageInboundOps(pageParams: PurchaseInboundPageParam) {
  return httpClient.get<ApiResult<PageResult<PurchaseInboundPageVO>>>(
    '/wms/inbound-execution/page',
    { params: pageParams }
  )
}

/** 平台入库单详情（收货/上架弹窗用，平台自有权限） */
export function getInboundOpsDetail(id: number) {
  return httpClient.get<ApiResult<PurchaseInboundDetailVO>>('/wms/inbound-execution/detail', {
    params: { id }
  })
}

export function scanInboundOrder(inboundNo: string) {
  return httpClient.get<ApiResult<PurchaseInboundDetailVO>>('/wms/inbound-execution/scan', {
    params: { inboundNo }
  })
}

/** 平台收货（录实收数量） */
export function receiveInbound(dto: InboundReceiveDTO) {
  return httpClient.post<ApiResult<void>>('/wms/inbound-execution/receive', dto)
}

/** 登记工作人员已经完成的实际放置结果。 */
export function recordPutaway(dto: PutawayRecordDTO) {
  return httpClient.post<ApiResult<PutawayReceiptLineVO[]>>(
    '/wms/inbound-execution/putaway-record',
    dto
  )
}

export function getPutawayRecordContext(inboundOrderId: number) {
  return httpClient.get<ApiResult<PutawayRecordContextVO>>(
    '/wms/inbound-execution/putaway-record-context',
    { params: { inboundOrderId } }
  )
}

/** 查询已完成入库单本次上架关联的托盘，用于详情与标签补打。 */
export function getPutawayPallets(inboundOrderId: number) {
  return httpClient.get<ApiResult<PalletSummaryVO[]>>(
    '/wms/inbound-execution/putaway-pallets',
    { params: { inboundOrderId } }
  )
}

/** 查询本张入库单实际写入的上架明细，用于上架单补打。 */
export function getPutawayReceiptLines(inboundOrderId: number) {
  return httpClient.get<ApiResult<PutawayReceiptLineVO[]>>(
    '/wms/inbound-execution/putaway-receipt-lines',
    { params: { inboundOrderId } }
  )
}

/** 上架可选库位（本货主服务商租用货架上、空闲 + 分区匹配品质；quality: GOOD/DAMAGED） */
export function listPutawayLocations(inboundOrderId: number, quality: string) {
  return httpClient.get<ApiResult<AvailableLocationVO[]>>(
    '/wms/inbound-execution/available-locations',
    { params: { inboundOrderId, quality } }
  )
}
