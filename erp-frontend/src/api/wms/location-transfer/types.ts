import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'
import type { PalletSummaryVO } from '@/api/wms/pallet'

// ==================== 状态 ====================

/** 库位调整单状态（唯一数据源）：平台新建待调整→执行完成/取消。 */
export const LocationTransferStatusList = [
  { value: 'PLANNED', label: '待选择库位', badge: 'warning' },
  { value: 'PENDING', label: '待调整', badge: 'processing' },
  { value: 'COMPLETED', label: '已完成', badge: 'success' },
  { value: 'CANCELLED', label: '已取消', badge: 'default' }
] as const

export type LocationTransferStatus = (typeof LocationTransferStatusList)[number]['value']

export const LocationTransferReasonList = [
  { value: 'LOCATION_ORGANIZATION', label: '库位整理' },
  { value: 'BACKLOG_STAGING', label: '积压暂存' },
  { value: 'OUTBOUND_PREPARATION', label: '出库准备' },
  { value: 'STOCKTAKE_CORRECTION', label: '盘点纠正' },
  { value: 'TEMPORARY_CLEARANCE', label: '临时腾位' },
  { value: 'OTHER', label: '其他' }
] as const

// ==================== 目标库位候选 ====================

export interface TargetLocationVO {
  locationId: number
  locationCode: string
  zoneId?: number
  zoneName?: string
  zoneType?: string
  rackNo?: string
  columnNo?: number
  /** 1=虚拟库位（收纳积压货用，服务商不可见） */
  isVirtual?: number
  targetType?: 'EMPTY_SLOT' | 'EXISTING_PALLET' | 'VIRTUAL'
  slotId?: number
  slotCode?: string
  palletId?: number
  palletNo?: string
}

// ==================== DTO ====================

/** 调整明细（一行=源批次移到一个目标库位） */
export interface LocationTransferItemDTO {
  physicalInventoryId: number
  quantity: number
  targetLocationCode: string
  moveMode?: 'PARTIAL' | 'WHOLE_PALLET'
  targetSlotId?: number
  targetPalletId?: number
  remark?: string
}

/** 新建库位调整单 DTO */
export interface LocationTransferCreateDTO {
  warehouseId: number
  erpTenantId: number
  reasonCode: string
  reason?: string
  remark?: string
  items: LocationTransferItemDTO[]
}

export interface LogicalLocationTransferCreateDTO {
  warehouseId: number
  erpTenantId: number
  reasonCode: string
  reason?: string
  remark?: string
  items: Array<{
    sourceInventoryId: number
    targetLocationId: number
    quantity: number
    remark?: string
  }>
}

export interface LogicalTransferSourceVO {
  inventoryId: number
  warehouseId: number
  wmsTenantId: number
  erpTenantId: number
  ownerName?: string
  skuCode: string
  warehouseSkuCode?: string
  quality: string
  quantity: number
  reservedQuantity: number
  availableQuantity: number
  locationId: number
  locationCode: string
  rackNo?: string
  locationType?: string
  zoneName?: string
  zoneType?: string
}

export interface LogicalTransferLocationVO {
  locationId: number
  locationCode: string
  rackNo?: string
  columnNo?: number
  locationType?: string
  zoneName?: string
  zoneType?: string
  publicShared?: number
  utilizationPercent?: number
}

export interface LocationTransferBatchCreateDTO {
  warehouseId: number
  reasonCode: string
  reason?: string
  remark?: string
  items: LocationTransferItemDTO[]
}

export interface LocationTransferSourceBatchVO {
  id: number
  warehouseId: number
  wmsTenantId?: number
  erpTenantId: number
  ownerName?: string
  skuCode: string
  warehouseSkuCode?: string
  quantity: number
  reservedQty: number
  quality: string
  inboundDate?: string
  locationCode: string
  zoneId?: number
  palletId?: number
  palletNo?: string
  palletStatus?: string
  palletType?: string
  slotId?: number
  slotCode?: string
}

// ==================== 查询/VO ====================

export interface LocationTransferQO {
  transferNo?: string
  warehouseId?: number
  erpTenantId?: number
  wmsTenantId?: number
  orderStatus?: LocationTransferStatus
  sourceType?: string
  operatorUserId?: number
  /** 创建日期起始 YYYY-MM-DD */
  createTimeStart?: string
  /** 创建日期结束 YYYY-MM-DD */
  createTimeEnd?: string
}

export type LocationTransferPageParam = LocationTransferQO & PageParam

/** 库位调整单分页视图对象 */
export interface LocationTransferPageVO {
  id: number
  transferNo: string
  warehouseId: number
  warehouseName?: string
  erpTenantId: number
  ownerName?: string
  wmsTenantId?: number
  operatorName?: string
  orderStatus: LocationTransferStatus
  sourceType?: string
  sourceId?: number
  sourceNo?: string
  reasonCode?: string
  reason?: string
  remark?: string
  itemCount: number
  totalQuantity: number
  completeTime?: string
  createBy?: number
  createByName?: string
  completeBy?: number
  operatorUserName?: string
  createTime: string
}

/** 库位调整单明细视图对象 */
export interface LocationTransferItemVO {
  id: number
  skuCode: string
  warehouseSkuCode?: string
  skuBrief?: SkuBriefVO
  physicalInventoryId?: number
  sourceInventoryId?: number
  sourceLocationCode?: string
  sourceZoneName?: string
  sourceQuality?: string
  moveMode?: 'PARTIAL' | 'WHOLE_PALLET'
  sourcePalletId?: number
  sourcePalletNo?: string
  sourceSlotId?: number
  sourceSlotCode?: string
  targetLocationCode?: string
  targetLocationId?: number
  targetZoneName?: string
  targetSlotId?: number
  targetSlotCode?: string
  targetPalletId?: number
  targetPalletNo?: string
  quantity: number
  toGood?: number
  remark?: string
}

/** 库位调整单详情视图对象 */
export interface LocationTransferDetailVO extends LocationTransferPageVO {
  items: LocationTransferItemVO[]
  printablePallets?: PalletSummaryVO[]
}

export interface LocationTransferPlanDTO {
  items: Array<{
    id: number
    targetLocationCode: string
  }>
}
