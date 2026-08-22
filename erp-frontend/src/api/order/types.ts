/**
 * 订单商品明细 VO（共享类型）
 */
export interface OrderItemVO {
  platformItemId: string
  skuCode: string
  skuName: string
  mainImage: string
  quantity: number
  itemPrice: number
  itemAmount: number
}

// 统一 ERP 订单状态定义与展示（供前端多个组件共用）
export type ErpStatusKey =
  | 'READY_TO_SHIP'
  | 'SHIPPED'
  | 'ARRIVED_AT_PLATFORM_WAREHOUSE'
  | 'DELIVERED'
  | 'CANCELED'
  | 'RETURNED'

// 统一映射：键即为服务端存储格式（大写+下划线）
export const ERP_STATUS_MAP: Record<ErpStatusKey, { label: string; tip: string; cls: string }> = {
  READY_TO_SHIP: { label: '待确认', tip: 'Ready to ship', cls: 'erp-ready' },
  SHIPPED: { label: '发货中', tip: 'Shipped', cls: 'erp-shipped' },
  ARRIVED_AT_PLATFORM_WAREHOUSE: {
    label: '已送仓',
    tip: 'Arrived At Platform Warehouse',
    cls: 'erp-arrived'
  },
  DELIVERED: { label: '已送达', tip: 'Delivered', cls: 'erp-delivered' },
  CANCELED: { label: '已取消', tip: 'Canceled', cls: 'erp-canceled' },
  RETURNED: { label: '已退货/拒收', tip: 'Returned', cls: 'erp-returned' }
}

/**
 * 订单基础 VO（三平台公共字段）
 */
export interface BaseOrderVO {
  id: number
  platform?: string
  shopId: number
  erpShopName?: string
  platformOrderId: string
  shipmentId?: string
  fulfillmentType?: string
  warehouseId?: string
  platformStatus: string
  platformSubstatus?: string
  erpStatus: string
  totalAmount?: number
  currencyCode?: string
  convertedAmount?: number
  convertedCurrencyCode?: string
  hasLabel: boolean
  locked: number
  fulfillmentOrderId?: number
  warehouseFulfillmentStatus?: string
  items: OrderItemVO[]
  /** SKU 总数（来自 erp_order 表，三平台共享） */
  skuCount?: number
  platformCreatedAt?: string
  platformCreatedAtMoscow?: string
  syncedAt?: string
  createTime?: string
  updateTime?: string
}
