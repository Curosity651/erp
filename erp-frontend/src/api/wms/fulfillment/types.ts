export type FulfillmentStatus =
  | 'DRAFT'
  | 'WAITING_SHELF'
  | 'PLATFORM_PROCESSING'
  | 'WAITING_PICK'
  | 'PICKING'
  | 'WAITING_PACK'
  | 'PACKED'
  | 'SHIPPED'
  | 'CANCEL_RETURNING'
  | 'CANCELLED'
  | 'EXCEPTION'

export interface FulfillmentOrder {
  id: number
  warehouseId: number
  fulfillmentNo: string
  sourceType: string
  sourceOrderNo: string
  fulfillmentStatus: FulfillmentStatus
  recipientName?: string
  recipientPhone?: string
  recipientAddress?: string
  createTime?: string
}

export interface FulfillmentItem {
  id: number
  fulfillmentOrderId: number
  skuCode: string
  warehouseSkuCode: string
  skuName?: string
  quantity: number
}

export interface ManualFulfillmentItem {
  skuCode: string
  quantity: number
}

export interface ManualFulfillmentForm {
  warehouseId?: number
  recipientName?: string
  recipientPhone?: string
  recipientAddress?: string
  items: ManualFulfillmentItem[]
}

export const fulfillmentStatusText: Record<FulfillmentStatus, string> = {
  DRAFT: '草稿',
  WAITING_SHELF: '待下架',
  PLATFORM_PROCESSING: '平台处理中',
  WAITING_PICK: '待拣货',
  PICKING: '拣货中',
  WAITING_PACK: '待打包',
  PACKED: '已打包',
  SHIPPED: '已完成',
  CANCEL_RETURNING: '取消回退中',
  CANCELLED: '已取消',
  EXCEPTION: '异常'
}

