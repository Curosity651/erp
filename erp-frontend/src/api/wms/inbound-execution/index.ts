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

/** 上架分配行 */
export interface PutawayLine {
  skuCode: string
  locationId: number
  overrideReason?: string
  locationCode?: string
  palletKey?: string
  palletId?: number
  slotCode?: string
  quantity: number
  quality?: string
  zoneId?: number
  capacityPercent?: number
  capacitySource?: string
  actualWeightKg?: number
  manualFull?: boolean
}

/** 上架 DTO */
export interface InboundPutawayDTO {
  inboundOrderId: number
  lines: PutawayLine[]
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

export interface LocationRecommendationVO {
  locationId: number
  locationCode: string
  rackNo?: string
  sequenceNo?: number
  locationType?: string
  zoneType?: string
  publicShared?: number
  recommendedQuantity: number
  maxByGeometry: number
  maxByVolume: number
  maxByWeight: number
  remainingVolumeMm3: number
  weightAllowed: boolean
  skuKindsAllowed: boolean
}

export interface LogicalPutawaySkuPlanVO {
  skuCode: string
  skuName?: string
  receivedQuantity: number
  outerLengthMm: number
  outerWidthMm: number
  outerHeightMm: number
  outerGrossWeightG: number
  recommendations: LocationRecommendationVO[]
}

export interface LogicalInboundPutawayPlanVO {
  inboundOrderId: number
  warehouseId: number
  items: LogicalPutawaySkuPlanVO[]
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

/** 平台上架（分配库位写批次） */
export function putawayInbound(dto: InboundPutawayDTO) {
  return httpClient.post<ApiResult<PutawayReceiptLineVO[]>>(
    '/wms/inbound-execution/logical-putaway',
    dto
  )
}

export function getPutawayPlan(inboundOrderId: number) {
  return httpClient.get<ApiResult<LogicalInboundPutawayPlanVO>>(
    '/wms/inbound-execution/logical-putaway-plan',
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
