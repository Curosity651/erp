import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

// ==================== 报废单状态 ====================

/**
 * 报废单状态（唯一数据源）：平台发起→待货主确认→货主确认销毁/驳回，平台可撤销。
 */
export const ScrapStatusList = [
  { value: 'PENDING_OWNER', label: '待货主确认', badge: 'processing' },
  { value: 'SCRAPPED', label: '已销毁', badge: 'success' },
  { value: 'REJECTED', label: '已驳回', badge: 'error' },
  { value: 'CANCELLED', label: '已取消', badge: 'default' }
] as const

/** 报废单状态 */
export type ScrapStatus = (typeof ScrapStatusList)[number]['value']

// ==================== 批次（源货物） ====================

/** 批次明细（/wms/physical-inventory/batches 返回的实体，报废/移库选源用） */
export interface PhysicalBatchVO {
  id: number
  skuCode: string
  locationCode: string
  quality: string // GOOD / DAMAGED
  quantity: number
  reservedQty: number
  zoneId?: number
  inboundDate?: string
  inboundItemId?: number
}

// ==================== DTO ====================

/** 报废明细（锁定一个批次） */
export interface ScrapItemDTO {
  physicalInventoryId: number
  skuCode: string
  quantity: number
  remark?: string
}

/** 发起报废 DTO */
export interface ScrapCreateDTO {
  warehouseId: number
  erpTenantId: number
  adjustmentDate?: string
  adjustmentReason?: string
  items: ScrapItemDTO[]
}

// ==================== 查询/VO ====================

/** 报废单查询对象 */
export interface AdjustmentQO {
  adjustmentNo?: string
  warehouseId?: number
  erpTenantId?: number
  wmsTenantId?: number
  orderStatus?: ScrapStatus
  adjustmentDateStart?: string
  adjustmentDateEnd?: string
}

export type AdjustmentPageParam = AdjustmentQO & PageParam

/** 报废单分页视图对象 */
export interface AdjustmentPageVO {
  id: number
  adjustmentNo: string
  adjustmentType: string
  warehouseId: number
  warehouseName: string
  erpTenantId: number
  ownerName?: string
  wmsTenantId?: number
  operatorName?: string
  adjustmentDate: string
  orderStatus: ScrapStatus
  adjustmentReason?: string
  rejectReason?: string
  skuCount: number
  totalQuantity: number
  confirmTime?: string
  createTime: string
}

/** 报废明细视图对象 */
export interface AdjustmentItemVO {
  id: number
  skuCode: string
  skuBrief?: SkuBriefVO
  physicalInventoryId?: number
  locationCode?: string
  quality?: string
  quantity: number
  remark?: string
}

/** 报废单详情视图对象 */
export interface AdjustmentDetailVO extends AdjustmentPageVO {
  items: AdjustmentItemVO[]
}
