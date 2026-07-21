import type { PageParam, PageResult } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 销售出库单 DTO
 */
export interface SalesOutboundDTO {
  id?: number
  warehouseId: number
  platform: string
  outboundDate: string
  remark?: string
  /** 物流产品ID（父服务商提供，选填；签出时按其单价计物流费） */
  logisticsProductId?: number
  logisticsProductName?: string
  channelName?: string
  trackingNo?: string
  weight?: number
  shippingFee?: number
  items: SalesOutboundItemDTO[]
}

/**
 * 销售出库单明细 DTO
 */
export interface SalesOutboundItemDTO {
  erpOrderId: number
  platformOrderId?: string
  skuCode: string
  quantity: number
  remark?: string
}

/**
 * 销售出库单查询对象
 */
export interface SalesOutboundQO {
  outboundNo?: string
  platformOrderId?: string
  warehouseId?: number
  platform?: string
  orderStatus?: string
  outboundDateStart?: string
  outboundDateEnd?: string
}

/**
 * 销售出库单分页参数
 */
export type SalesOutboundPageParam = SalesOutboundQO & PageParam

/**
 * 销售出库单分页 VO
 */
export interface SalesOutboundPageVO {
  id: number
  outboundNo: string
  warehouseId: number
  warehouseName: string
  platform: string
  outboundDate: string
  orderCount: number
  skuCount: number
  totalQuantity: number
  orderStatus: string
  remark?: string
  createBy?: number
  createByName?: string
  createTime: string
}

/**
 * 销售出库单详情 VO
 */
export interface SalesOutboundDetailVO extends SalesOutboundPageVO {
  items: SalesOutboundItemVO[]
  /** 物流产品ID（父服务商提供） */
  logisticsProductId?: number
  /** 是否存在库存不足 */
  hasStockShortage?: boolean
  /** 库存不足的SKU数量 */
  shortageSkuCount?: number
}

/**
 * 销售出库单明细 VO
 */
export interface SalesOutboundItemVO {
  id: number
  outboundOrderId: number
  erpOrderId: number
  platformOrderId: string
  skuCode: string
  skuBrief?: SkuBriefVO
  quantity: number
  remark?: string
  /** 可用库存数量 */
  availableStock?: number
  /** 库存缺口 */
  shortage?: number
  /** 库存状态 */
  stockStatus?: StockStatus
}

/**
 * 待出库订单查询对象
 */
export interface PendingOrderQO {
  platform: string
  warehouseId: number
  keyword?: string
  skuCodes?: string[]
}

/**
 * 待出库订单分页参数
 */
export type PendingOrderPageParam = PendingOrderQO & PageParam

/**
 * 待出库订单明细 VO
 */
export interface PendingOrderItemVO {
  platformItemId: string
  skuCode: string
  skuBrief?: SkuBriefVO
  quantity: number
  availableStock?: number
  stockStatus?: 'sufficient' | 'insufficient' | 'zero'
}

/**
 * 待出库订单 VO
 */
export interface PendingOrderVO {
  id: number
  platformOrderId: string
  platform: string
  shopId: number
  shopName: string
  orderTime: string
  items: PendingOrderItemVO[]
  /** 商品总数量 */
  totalQuantity: number
  /** 可用库存数量（取所有 item 中最低值） */
  availableStock?: number
  /** 库存状态 */
  stockStatus?: 'sufficient' | 'insufficient' | 'zero'
}

/**
 * 出库单状态枚举
 */
export enum OutboundOrderStatus {
  DRAFT = 'DRAFT',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  PICKING = 'PICKING',
  BACKORDER = 'BACKORDER',
  PACKED = 'PACKED',
  SHIPPED = 'SHIPPED'
}

/**
 * 出库单状态描述映射
 */
export const OutboundOrderStatusMap: Record<string, string> = {
  [OutboundOrderStatus.DRAFT]: '草稿',
  [OutboundOrderStatus.CONFIRMED]: '待下架',
  [OutboundOrderStatus.CANCELLED]: '已取消',
  [OutboundOrderStatus.PICKING]: '拣货中',
  [OutboundOrderStatus.BACKORDER]: '缺货挂起',
  [OutboundOrderStatus.PACKED]: '已打包',
  [OutboundOrderStatus.SHIPPED]: '已完成'
}

/**
 * 库存不足明细 VO
 */
export interface StockShortageVO {
  skuCode: string
  skuName: string
  requiredQty: number
  availableQty: number
  shortage: number
}

/**
 * 库存状态类型
 */
export type StockStatus = 'sufficient' | 'insufficient' | 'zero' | 'deducted'
