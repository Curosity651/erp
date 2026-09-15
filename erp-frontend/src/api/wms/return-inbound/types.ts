import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 退货入库单 DTO（简化版，无 items 数组）
 */
export interface ReturnInboundDTO {
  /** 订单商品明细ID */
  orderItemId: number
  warehouseId: number
  returnDate: string
  returnReason: string
  remark?: string
  quantity: number
}

/**
 * 退货入库单查询对象
 */
export interface ReturnInboundQO {
  returnNo?: string
  platformOrderId?: string
  platform?: string
  skuCode?: string
  warehouseId?: number
  returnDateStart?: string
  returnDateEnd?: string
  returnStatus?: string
}

/**
 * 退货入库单分页参数
 */
export type ReturnInboundPageParam = ReturnInboundQO & PageParam

/**
 * 退货入库单分页 VO
 */
export interface ReturnInboundPageVO {
  id: number
  returnNo: string
  returnBatchNo?: string
  skuKinds?: number
  erpOrderId: number
  platformOrderId: string
  platform: string
  skuCode: string
  warehouseId: number
  warehouseName: string
  returnDate: string
  returnReason?: string
  totalQuantity: number
  qualifiedQuantity: number
  unqualifiedQuantity: number
  toDamagedQuantity: number
  scrapQuantity: number
  returnStatus: 'PENDING_OWNER' | 'PENDING_OPERATION' | 'COMPLETED' | 'CLOSED'
  skuBrief: SkuBriefVO
  remark?: string
  createBy?: number
  createByName?: string
  createTime: string
}

/**
 * 退货入库单详情 VO（简化版，无 items 数组）
 */
export interface ReturnInboundDetailVO extends ReturnInboundPageVO {
  returnableQuantity: number // 可退数量快照
  items: ReturnDispositionItemVO[]
}

export interface ReturnDispositionItemVO {
  id: number
  skuCode: string
  warehouseSkuCode?: string
  skuName?: string
  platformOrderId?: string
  returnReason?: string
  receivedQty: number
  restockQty?: number
  reworkQty?: number
  scrapQty?: number
  reworkPassQty?: number
  reworkScrapQty?: number
  dispositionRemark?: string
  processedLocationCode?: string
  qcPhotoFileIds?: number[]
  qcPhotoFileIds?: number[]
}

export interface ReturnDispositionDTO {
  returnOrderId: number
  items: {
    itemId: number
    restockQty: number
    reworkQty: number
    scrapQty: number
    remark?: string
  }[]
}

/**
 * 可退货订单 VO（item 级别，每行一个可退货 item）
 */
export interface ReturnableOrderVO {
  orderId: number
  platformOrderId: string
  platform: string
  outboundTime: string
  /** item 级别 */
  orderItemId: number
  platformItemId: string
  skuCode: string
  skuBrief: SkuBriefVO
  shippedQuantity: number
  returnedQuantity: number
  inFlightQuantity: number
  returnableQuantity: number
}

/**
 * 可退货订单查询对象
 */
export interface ReturnableOrderQO {
  keyword?: string
  platform?: string
}

/**
 * 可退货订单分页参数
 */
export type ReturnableOrderPageParam = ReturnableOrderQO & PageParam

/**
 * 退货原因枚举
 */
export enum ReturnReason {
  NOT_WANTED = 'NOT_WANTED',
  DAMAGED = 'DAMAGED',
  WRONG_ITEM = 'WRONG_ITEM',
  QUALITY_ISSUE = 'QUALITY_ISSUE',
  OTHER = 'OTHER'
}

/**
 * 退货原因描述映射
 */
export const ReturnReasonMap: Record<string, string> = {
  [ReturnReason.NOT_WANTED]: '客户不想要了',
  [ReturnReason.DAMAGED]: '商品破损',
  [ReturnReason.WRONG_ITEM]: '发错货',
  [ReturnReason.QUALITY_ISSUE]: '质量问题',
  [ReturnReason.OTHER]: '其他'
}
