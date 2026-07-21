import type { PageParam } from '@/api/types'
import type { BaseOrderVO } from '@/api/order/types'

// 从共用类型文件导出 ERP 统一状态映射
export { ERP_STATUS_MAP } from '@/api/order/types'
export type { ErpStatusKey } from '@/api/order/types'

/**
 * Yandex 订单查询对象
 */
export interface YdOrderQO {
  id?: number
  shopId?: number
  platform?: string
  platformOrderId?: string
  erpStatus?: string
  platformStatus?: string
  platformSubstatus?: string
  skuCode?: string
  createdAtStart?: string
  createdAtEnd?: string
}

/**
 * Yandex 订单分页参数
 */
export type YdOrderPageParam = YdOrderQO & PageParam

/**
 * Yandex 订单分页视图对象
 */
export interface YdOrderPageVO extends BaseOrderVO {
  // Yandex 特有金额字段
  productTotalAmount?: number
  productCurrencyCode?: string
  productAmountCny?: number
}

/**
 * Yandex 平台主状态映射
 */
export const YD_STATUS_MAP: Record<string, { label: string; tip: string; cls: string }> = {
  PROCESSING: { label: '处理中', tip: 'Processing - 订单正在处理', cls: 'processing' },
  DELIVERY: { label: '配送中', tip: 'Delivery - 已移交配送', cls: 'processing' },
  PICKUP: { label: '待取货', tip: 'Pickup - 到达取货点', cls: 'processing' },
  DELIVERED: { label: '已送达', tip: 'Delivered - 已签收', cls: 'success' },
  CANCELLED: { label: '已取消', tip: 'Cancelled - 订单已取消', cls: 'gray' },
  PARTIALLY_RETURNED: { label: '部分退货', tip: 'Partially Returned - 部分商品已退货', cls: 'warning' },
  RETURNED: { label: '已退货', tip: 'Returned - 全部商品已退货', cls: 'gray' }
}

/**
 * Yandex 子状态映射
 */
// 面单批次 VO（复用公共类型）
export type { LabelBatchVO } from '@/api/order/label-batch'

export const YD_SUBSTATUS_MAP: Record<string, { label: string; tip: string; cls: string }> = {
  // ======== PROCESSING 阶段 ========
  STARTED: { label: '新订单', tip: 'Started - 新订单待处理', cls: 'processing' },
  READY_TO_SHIP: { label: '待发货', tip: 'Ready to Ship - 已装箱，待发货', cls: 'processing' },
  SHIPPED: { label: '已发出', tip: 'Shipped - 已移交承运', cls: 'processing' },
  // ======== CANCELLED 原因 ========
  RESERVATION_EXPIRED: {
    label: '预留过期',
    tip: 'Reservation Expired - 买家未完成下单',
    cls: 'gray'
  },
  USER_NOT_PAID: {
    label: '未支付',
    tip: 'User Not Paid - 买家未支付',
    cls: 'gray'
  },
  USER_UNREACHABLE: {
    label: '无法联系',
    tip: 'User Unreachable - 无法联系买家',
    cls: 'gray'
  },
  USER_CHANGED_MIND: {
    label: '买家取消',
    tip: 'User Changed Mind - 买家主动取消',
    cls: 'gray'
  },
  USER_REFUSED_DELIVERY: {
    label: '拒绝配送',
    tip: 'User Refused Delivery - 不满意配送条件',
    cls: 'gray'
  },
  USER_REFUSED_PRODUCT: {
    label: '拒绝商品',
    tip: 'User Refused Product - 不满意商品',
    cls: 'gray'
  },
  SHOP_FAILED: {
    label: '商家取消',
    tip: 'Shop Failed - 商家无法履约',
    cls: 'gray'
  },
  USER_REFUSED_QUALITY: {
    label: '质量不满',
    tip: 'User Refused Quality - 不满意商品质量',
    cls: 'gray'
  },
  REPLACING_ORDER: {
    label: '换货取消',
    tip: 'Replacing Order - 买家要求换货',
    cls: 'gray'
  },
  PROCESSING_EXPIRED: {
    label: '处理超时',
    tip: 'Processing Expired - 处理超时(已废弃)',
    cls: 'gray'
  },
  PICKUP_EXPIRED: {
    label: '存储到期',
    tip: 'Pickup Expired - 取货点存储到期',
    cls: 'gray'
  },
  DELIVERY_SERVICE_UNDELIVERED: {
    label: '配送失败',
    tip: 'Delivery Service Undelivered - 配送服务无法送达',
    cls: 'gray'
  },
  CANCELLED_COURIER_NOT_FOUND: {
    label: '无快递员',
    tip: 'Cancelled Courier Not Found - 找不到快递员',
    cls: 'gray'
  },
  USER_WANTS_TO_CHANGE_DELIVERY_DATE: {
    label: '改配送日期',
    tip: 'User Wants to Change Delivery Date - 买家想更换配送日期',
    cls: 'gray'
  },
  RESERVATION_FAILED: {
    label: '平台异常',
    tip: 'Reservation Failed - 平台无法继续处理',
    cls: 'gray'
  }
}
