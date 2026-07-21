import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 调拨类型枚举（系统仅保留普通调拨一种）
 */
export enum TransferType {
  /** 普通调拨 */
  NORMAL = 'NORMAL'
}

/**
 * 调拨单状态枚举
 */
export enum TransferOrderStatus {
  /** 草稿 */
  DRAFT = 'DRAFT',
  /** 在途 */
  IN_TRANSIT = 'IN_TRANSIT',
  /** 已入库 */
  COMPLETED = 'COMPLETED',
  /** 已取消 */
  CANCELLED = 'CANCELLED',
  /** 已撤回 */
  REVOKED = 'REVOKED'
}

/**
 * 调拨单状态描述映射
 */
export const TransferOrderStatusMap: Record<string, string> = {
  [TransferOrderStatus.DRAFT]: '草稿',
  [TransferOrderStatus.IN_TRANSIT]: '在途',
  [TransferOrderStatus.COMPLETED]: '已入库',
  [TransferOrderStatus.CANCELLED]: '已取消',
  [TransferOrderStatus.REVOKED]: '已撤回'
}

/**
 * 调拨单状态 Badge 映射（用于列表页 Badge）
 */
export const TransferOrderStatusBadgeMap: Record<string, string> = {
  [TransferOrderStatus.DRAFT]: 'default',
  [TransferOrderStatus.IN_TRANSIT]: 'processing',
  [TransferOrderStatus.COMPLETED]: 'success',
  [TransferOrderStatus.CANCELLED]: 'error',
  [TransferOrderStatus.REVOKED]: 'warning'
}

/**
 * 调拨单状态颜色映射（用于详情页 Tag）
 */
export const TransferOrderStatusColorMap: Record<string, string> = {
  [TransferOrderStatus.DRAFT]: 'default',
  [TransferOrderStatus.IN_TRANSIT]: 'processing',
  [TransferOrderStatus.COMPLETED]: 'success',
  [TransferOrderStatus.CANCELLED]: 'error',
  [TransferOrderStatus.REVOKED]: 'warning'
}

/**
 * 调拨单状态元数据
 */
export const TransferOrderStatusMeta: Record<
  TransferOrderStatus,
  {
    label: string
    badge: string
    color: string
    timePrefix: string
    endTimeLabel: string
    timeField: 'createTime' | 'shipTime' | 'endTime'
  }
> = {
  [TransferOrderStatus.DRAFT]: {
    label: '草稿',
    badge: 'default',
    color: 'default',
    timePrefix: '创建于',
    endTimeLabel: '结束时间',
    timeField: 'createTime'
  },
  [TransferOrderStatus.IN_TRANSIT]: {
    label: '在途',
    badge: 'processing',
    color: 'processing',
    timePrefix: '出库于',
    endTimeLabel: '结束时间',
    timeField: 'shipTime'
  },
  [TransferOrderStatus.COMPLETED]: {
    label: '已完成',
    badge: 'success',
    color: 'success',
    timePrefix: '入库于',
    endTimeLabel: '入库时间',
    timeField: 'endTime'
  },
  [TransferOrderStatus.CANCELLED]: {
    label: '已取消',
    badge: 'error',
    color: 'error',
    timePrefix: '取消于',
    endTimeLabel: '取消时间',
    timeField: 'endTime'
  },
  [TransferOrderStatus.REVOKED]: {
    label: '已撤回',
    badge: 'warning',
    color: 'warning',
    timePrefix: '撤回于',
    endTimeLabel: '撤回时间',
    timeField: 'endTime'
  }
}

/**
 * 调拨单 DTO
 */
export interface TransferOrderDTO {
  id?: number
  transferType: string
  fromWarehouseId: number
  toWarehouseId: number
  /** 货主（货物归属），整单归属一个货主 */
  erpTenantId: number
  remark?: string
  items: TransferOrderItemDTO[]
  /** 是否确认出库（新建时使用） */
  confirmShip?: boolean
}

/**
 * 调拨单明细 DTO
 */
export interface TransferOrderItemDTO {
  /** 源批次ID（A仓，发出扣此批次） */
  sourcePhysicalInventoryId: number
  /** 目标库位编码（B仓，须落在该货主服务商租用排） */
  targetLocationCode: string
  /** SKU（可空，后端从源批次带出） */
  skuCode?: string
  quantity: number
  remark?: string
}

/** 源批次候选（A仓） */
export interface TransferSourceBatchVO {
  id: number
  erpTenantId: number
  warehouseId: number
  skuCode: string
  quantity: number
  reservedQty?: number
  locationCode?: string
  inboundDate?: string
}

/** 目标库位候选（B仓·服务商租用标准库位） */
export interface TransferTargetLocationVO {
  locationId: number
  locationCode: string
  zoneName?: string
  rackNo?: string
}

/**
 * 调拨单查询对象
 */
export interface TransferOrderQO {
  transferNo?: string
  transferType?: string
  fromWarehouseId?: number
  toWarehouseId?: number
  orderStatus?: string
  createTimeStart?: string
  createTimeEnd?: string
}

/**
 * 调拨单分页参数
 */
export type TransferOrderPageParam = TransferOrderQO & PageParam

/**
 * 调拨单分页 VO
 */
export interface TransferOrderPageVO {
  id: number
  transferNo: string
  transferType: string
  fromWarehouseId: number
  fromWarehouseName: string
  toWarehouseId: number
  toWarehouseName: string
  orderStatus: string
  shipTime?: string
  endTime?: string
  skuCount: number
  totalQuantity: number
  createTime: string
}

/**
 * 调拨单详情 VO
 */
export interface TransferOrderDetailVO extends TransferOrderPageVO {
  remark?: string
  /** 货主（编辑回填用，可空；后端暂未回填时编辑需重选） */
  erpTenantId?: number
  items: TransferOrderItemVO[]
}

/**
 * 调拨单明细 VO
 */
export interface TransferOrderItemVO {
  id: number
  skuCode: string
  skuBrief: SkuBriefVO
  sourcePhysicalInventoryId?: number
  targetLocationCode?: string
  quantity: number
  receivedQuantity?: number
  remark?: string
}

/**
 * 可调拨库存 VO
 */
export interface AvailableStockVO {
  skuCode: string
  skuBrief?: SkuBriefVO
  availableQuantity: number
}

/**
 * 可调拨库存分页查询参数
 */
export interface AvailableStockPageParam extends PageParam {
  warehouseId: number
  keyword?: string
}

/**
 * 批量查询库存参数
 */
export interface BatchStockQueryDTO {
  warehouseId: number
  skuCodes: string[]
}
