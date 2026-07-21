import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 自定义出库单 DTO（不挂平台订单，明细直接 SKU + 数量）
 */
export interface CustomOutboundDTO {
  id?: number
  warehouseId: number
  outboundDate: string
  /** 出库类型 */
  customType: string
  /** 关联单号文本（线下订单号/退供单号等，可选） */
  refNo?: string
  /** 收件人姓名（可选，销毁类无收货人） */
  receiverName?: string
  /** 收件人电话（可选） */
  receiverPhone?: string
  /** 收货地址（可选） */
  receiverAddress?: string
  /** 物流产品ID（需要运输和计费时选择） */
  logisticsProductId?: number
  remark?: string
  items: CustomOutboundItemDTO[]
}

/**
 * 自定义出库单明细 DTO
 */
export interface CustomOutboundItemDTO {
  skuCode: string
  quantity: number
  remark?: string
}

/**
 * 自定义出库单查询对象
 */
export interface CustomOutboundQO {
  outboundNo?: string
  customType?: string
  skuCode?: string
  warehouseId?: number
  orderStatus?: string
  outboundDateStart?: string
  outboundDateEnd?: string
}

/**
 * 自定义出库单分页参数
 */
export type CustomOutboundPageParam = CustomOutboundQO & PageParam

/**
 * 自定义出库单分页 VO
 */
export interface CustomOutboundPageVO {
  id: number
  outboundNo: string
  customType: string
  refNo?: string
  warehouseId: number
  warehouseName: string
  outboundDate: string
  skuCount: number
  totalQuantity: number
  orderStatus: string
  remark?: string
  createBy?: number
  createTime: string
}

/**
 * 自定义出库单详情 VO
 */
export interface CustomOutboundDetailVO extends CustomOutboundPageVO {
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  postingId?: number
  logisticsProductId?: number
  logisticsProductName?: string
  channelName?: string
  trackingNo?: string
  weight?: number
  shippingFee?: number
  items: CustomOutboundItemVO[]
  /** 是否存在库存不足（草稿态计算） */
  hasStockShortage?: boolean
  /** 库存不足的SKU数量（草稿态计算） */
  shortageSkuCount?: number
}

/**
 * 自定义出库单明细 VO
 */
export interface CustomOutboundItemVO {
  id: number
  outboundOrderId: number
  skuCode: string
  skuBrief?: SkuBriefVO
  quantity: number
  remark?: string
  /** 可用库存数量（草稿态计算） */
  availableStock?: number
  /** 库存缺口 */
  shortage?: number
  /** 库存状态 */
  stockStatus?: string
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
 * 批量查询可售库存 DTO
 */
export interface BatchStockQueryDTO {
  warehouseId: number
  skuCodes: string[]
}

/**
 * 自定义出库类型枚举
 */
export enum CustomOutboundType {
  OFFLINE_ORDER = 'OFFLINE_ORDER',
  SAMPLE_SEND = 'SAMPLE_SEND',
  SCRAP = 'SCRAP',
  RETURN_TO_SUPPLIER = 'RETURN_TO_SUPPLIER',
  OTHER = 'OTHER'
}

/**
 * 自定义出库类型描述映射
 */
export const CustomOutboundTypeMap: Record<string, string> = {
  [CustomOutboundType.OFFLINE_ORDER]: '线下订单',
  [CustomOutboundType.SAMPLE_SEND]: '样品寄送',
  [CustomOutboundType.SCRAP]: '销毁报废',
  [CustomOutboundType.RETURN_TO_SUPPLIER]: '退供应商',
  [CustomOutboundType.OTHER]: '其他'
}

/**
 * 自定义出库类型 Tag 颜色映射
 */
export const CustomOutboundTypeColorMap: Record<string, string> = {
  [CustomOutboundType.OFFLINE_ORDER]: 'blue',
  [CustomOutboundType.SAMPLE_SEND]: 'cyan',
  [CustomOutboundType.SCRAP]: 'red',
  [CustomOutboundType.RETURN_TO_SUPPLIER]: 'orange',
  [CustomOutboundType.OTHER]: 'default'
}

/**
 * 自定义出库单状态枚举
 * SHIPPED 是签出后的最终状态，界面统一显示为“已完成”。
 */
export enum CustomOutboundStatus {
  DRAFT = 'DRAFT',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  PICKING = 'PICKING',
  BACKORDER = 'BACKORDER',
  PACKED = 'PACKED',
  SHIPPED = 'SHIPPED'
}

/**
 * 自定义出库单状态描述映射
 */
export const CustomOutboundStatusMap: Record<string, string> = {
  [CustomOutboundStatus.DRAFT]: '草稿',
  [CustomOutboundStatus.CONFIRMED]: '待下架',
  [CustomOutboundStatus.CANCELLED]: '已取消',
  [CustomOutboundStatus.PICKING]: '拣货中',
  [CustomOutboundStatus.BACKORDER]: '缺货挂起',
  [CustomOutboundStatus.PACKED]: '已打包',
  [CustomOutboundStatus.SHIPPED]: '已完成'
}
