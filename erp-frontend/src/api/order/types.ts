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

export type OwnerOrderBusinessStatus =
  | 'PENDING_CONFIRM'
  | 'WAITING_SHELF'
  | 'OUTBOUND_PROCESSING'
  | 'HANDED_OVER'
  | 'CANCELLED'
  | 'EXCEPTION'

export const OWNER_ORDER_STATUS_MAP: Record<
  OwnerOrderBusinessStatus,
  { label: string; tip: string; cls: string; dotClass: string }
> = {
  PENDING_CONFIRM: {
    label: '待确认',
    tip: '订单尚未提交海外仓',
    cls: 'pending',
    dotClass: 'status-pending'
  },
  WAITING_SHELF: {
    label: '待下架',
    tip: '已提交海外仓，等待确认下架',
    cls: 'warning',
    dotClass: 'status-readytoship'
  },
  OUTBOUND_PROCESSING: {
    label: '出库作业中',
    tip: '仓库正在拣货、贴面单、打包或等待签出',
    cls: 'processing',
    dotClass: 'status-shipped'
  },
  HANDED_OVER: {
    label: '已交接',
    tip: '已在打包签出环节完成交接',
    cls: 'success',
    dotClass: 'status-delivered'
  },
  CANCELLED: {
    label: '已取消',
    tip: '订单或仓库履约已取消',
    cls: 'gray',
    dotClass: 'status-canceled'
  },
  EXCEPTION: {
    label: '异常',
    tip: '仓库履约处理异常',
    cls: 'error',
    dotClass: 'status-canceled'
  }
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
  businessStatus?: OwnerOrderBusinessStatus
  totalAmount?: number
  currencyCode?: string
  convertedAmount?: number
  convertedCurrencyCode?: string
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
