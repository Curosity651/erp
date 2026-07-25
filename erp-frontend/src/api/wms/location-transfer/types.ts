import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

// ==================== 状态 ====================

/** 库位调整单状态（唯一数据源）：平台新建待调整→执行完成/取消。 */
export const LocationTransferStatusList = [
  { value: 'PLANNED', label: '待选择库位', badge: 'warning' },
  { value: 'PENDING', label: '待调整', badge: 'processing' },
  { value: 'COMPLETED', label: '已完成', badge: 'success' },
  { value: 'CANCELLED', label: '已取消', badge: 'default' }
] as const

export type LocationTransferStatus = (typeof LocationTransferStatusList)[number]['value']

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
}

// ==================== DTO ====================

/** 调整明细（一行=源批次移到一个目标库位） */
export interface LocationTransferItemDTO {
  physicalInventoryId: number
  quantity: number
  targetLocationCode: string
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

// ==================== 查询/VO ====================

export interface LocationTransferQO {
  transferNo?: string
  warehouseId?: number
  erpTenantId?: number
  wmsTenantId?: number
  orderStatus?: LocationTransferStatus
  sourceType?: string
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
  createTime: string
}

/** 库位调整单明细视图对象 */
export interface LocationTransferItemVO {
  id: number
  skuCode: string
  skuBrief?: SkuBriefVO
  physicalInventoryId?: number
  sourceLocationCode?: string
  sourceQuality?: string
  targetLocationCode?: string
  quantity: number
  toGood?: number
  remark?: string
}

/** 库位调整单详情视图对象 */
export interface LocationTransferDetailVO extends LocationTransferPageVO {
  items: LocationTransferItemVO[]
}

export interface LocationTransferPlanDTO {
  items: Array<{
    id: number
    targetLocationCode: string
  }>
}
