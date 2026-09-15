import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'
import type { RegionDisplayVO } from '@/api/wms/stock-flow/types'

/**
 * 采购入库单 DTO
 */
export interface PurchaseInboundDTO {
  id?: number
  inboundNo: string
  shippingOrderId: number
  warehouseId: number
  inboundDate: string
  remark?: string
  items: PurchaseInboundItemDTO[]
}

/**
 * 采购入库单明细 DTO
 */
export interface PurchaseInboundItemDTO {
  shippingOrderItemId: number
  purchaseOrderId: number
  purchaseOrderItemId: number
  skuCode: string
  expectedQuantity: number
  actualQuantity: number
  remark?: string
}

/**
 * 采购入库单查询对象
 */
export interface PurchaseInboundQO {
  inboundNo?: string
  shippingOrderNo?: string
  purchaseOrderNo?: string
  skuCode?: string
  warehouseId?: number
  orderStatus?: string
  orderStatuses?: string[]
  inboundDateStart?: string
  inboundDateEnd?: string
  /** 货主筛选（仅平台身份有效） */
  erpTenantId?: number
  /** WMS服务商筛选（仅平台身份有效） */
  wmsTenantId?: number
  receiveBy?: number
  putawayBy?: number
}

/**
 * 采购入库单分页参数
 */
export type PurchaseInboundPageParam = PurchaseInboundQO & PageParam

/**
 * 采购入库单分页 VO
 */
export interface PurchaseInboundPageVO {
  id: number
  inboundNo: string
  /** 货主ID */
  erpTenantId?: number
  /** 货主名称 */
  ownerName?: string
  ownerCode?: string
  /** 所属WMS服务商ID */
  operatorId?: number
  /** 所属WMS服务商名称 */
  operatorName?: string
  shippingOrderId: number
  shippingOrderNo: string
  providerName?: string
  warehouseId: number
  warehouseName: string
  inboundDate: string
  orderStatus: string
  receiveBy?: number
  receiveByName?: string
  receiveTime?: string
  putawayBy?: number
  putawayByName?: string
  putawayTime?: string
  itemSummary: string
  totalExpectedQty?: number
  totalActualQty?: number
  totalShortQty?: number
  skuCount?: number
  purchaseOrderNos?: string[]
  createTime: string
}

/**
 * 采购入库单详情 VO
 */
export interface PurchaseInboundDetailVO extends PurchaseInboundPageVO {
  remark?: string
  createByName?: string
  items: PurchaseInboundItemVO[]
  /** 关联物流单信息（用于编辑页回填） */
  shippingOrder?: AvailableShippingVO
}

/**
 * 采购入库单明细 VO
 */
export interface PurchaseInboundItemVO {
  id: number
  shippingOrderItemId: number
  purchaseOrderId: number
  purchaseOrderNo: string
  purchaseOrderItemId: number
  skuCode: string
  warehouseSkuCode?: string
  /** SKU简要信息 */
  skuBrief?: SkuBriefVO
  expectedQuantity: number
  /** 外箱尺寸（毫米），来源于 SKU 资料 */
  outerLengthMm?: number
  outerWidthMm?: number
  outerHeightMm?: number
  actualQuantity: number
  shortQuantity: number
  remark?: string
}

/**
 * 可入库物流单查询对象
 */
export interface AvailableShippingQO {
  shippingNo?: string
  purchaseOrderNo?: string
  providerId?: number
}

/**
 * 可入库物流单 VO
 */
export interface AvailableShippingVO {
  id: number
  shippingNo: string
  providerId: number
  providerName: string
  shippingDate: string
  totalQuantity: number
  receivedQuantity: number
  pendingQuantity: number
  targetRegionId: number
  /** 目标区域信息 */
  targetRegion?: RegionDisplayVO
}

/**
 * 物流单待入库明细 VO
 */
export interface ShippingItemForInboundVO {
  shippingOrderItemId: number
  purchaseOrderId: number
  purchaseOrderNo: string
  purchaseOrderItemId: number
  skuCode: string
  /** SKU简要信息 */
  skuBrief?: SkuBriefVO
  shippedQuantity: number
  receivedQuantity: number
  pendingQuantity: number
  expectedDeliveryDate?: string
}

/**
 * 入库单状态枚举（去中转层后的状态机：草稿→已提交→已收货→已完成 / 已取消）
 */
export enum InboundStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  RECEIVED = 'RECEIVED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

/**
 * 入库单状态描述映射
 */
export const InboundStatusMap: Record<string, string> = {
  [InboundStatus.DRAFT]: '草稿',
  [InboundStatus.SUBMITTED]: '已提交',
  [InboundStatus.RECEIVED]: '已收货',
  [InboundStatus.COMPLETED]: '已完成',
  [InboundStatus.CANCELLED]: '已取消'
}

/**
 * 入库单状态颜色映射（用于 Tag 组件）
 */
export const InboundStatusColorMap: Record<string, string> = {
  [InboundStatus.DRAFT]: 'default',
  [InboundStatus.SUBMITTED]: 'processing',
  [InboundStatus.RECEIVED]: 'warning',
  [InboundStatus.COMPLETED]: 'success',
  [InboundStatus.CANCELLED]: 'error'
}

/**
 * 入库明细表单数据（用于表单页面）
 */
export interface InboundItemFormData {
  shippingOrderItemId: number
  purchaseOrderId: number
  purchaseOrderNo: string
  purchaseOrderItemId: number
  skuCode: string
  /** SKU简要信息 */
  skuBrief?: SkuBriefVO
  expectedQuantity: number
  actualQuantity: number
  shortQuantity: number
  selected: boolean
  expectedDeliveryDate?: string
  remark?: string
}
