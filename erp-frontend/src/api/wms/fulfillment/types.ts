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
  erpTenantId: number
  warehouseId: number
  fulfillmentNo: string
  sourceType: string
  sourceOrderNo: string
  fulfillmentStatus: FulfillmentStatus
  recipientName?: string
  recipientPhone?: string
  recipientAddress?: string
  carrierName?: string
  shippingMethod?: string
  trackingNo?: string
  packageWeightKg?: number
  labelFileUrl?: string
  labelBarcode?: string
  labelFetchedTime?: string
  labelVerifiedTime?: string
  logisticsProductId?: number
  logisticsProductCode?: string
  logisticsProductName?: string
  logisticsProductDescription?: string
  logisticsProductDefaultFee?: number
  logisticsProductActualFee?: number
  logisticsProductCurrency?: string
  logisticsFeeAdjustmentReason?: string
  createTime?: string
}

export interface FulfillmentShelfOrderQuery {
  startTime?: string
  endTime?: string
  erpTenantId?: number
  logisticsProductId?: number
  warehouseId?: number
}

export interface FulfillmentBatchResult {
  successIds: number[]
  failures: Record<string, string>
}

export interface FulfillmentCreatedTask {
  taskId: number
  taskNo: string
  warehouseId: number
  orderCount: number
  totalQuantity: number
}

export interface FulfillmentDispatchResult extends FulfillmentBatchResult {
  tasks: FulfillmentCreatedTask[]
}

export interface FulfillmentPickTask {
  id: number
  taskNo: string
  warehouseId: number
  taskStatus: import('@/views/platform/fulfillment-picking/picking-task-flow').PickingTaskStatus
  orderCount: number
  totalQuantity: number
  operatorId?: number
  claimedTime?: string
  createTime?: string
  completedOrderCount?: number
  exceptionOrderCount?: number
}

export interface FulfillmentPickTaskOrder {
  id: number
  fulfillmentOrderId: number
  sequenceNo: number
  orderStatus: import('@/views/platform/fulfillment-picking/picking-task-flow').PickingOrderStatus
  previousOrderStatus?: string
  previousFulfillmentStatus?: string
  exceptionType?: string
  exceptionReason?: string
  exceptionImageUrls?: string
  startedTime?: string
  completedTime?: string
}

export interface FulfillmentPickTaskLine {
  id: number
  fulfillmentOrderId: number
  locationCode: string
  skuCode: string
  warehouseSkuCode: string
  plannedQuantity: number
  pickedQuantity: number
  lineStatus: string
}

export interface FulfillmentPickTaskDetail {
  task: FulfillmentPickTask
  orders: FulfillmentPickTaskOrder[]
  lines: FulfillmentPickTaskLine[]
  orderQueue: FulfillmentPickTaskOrderDetail[]
  currentOrder?: {
    taskOrder: FulfillmentPickTaskOrder
    fulfillmentOrder: FulfillmentOrder
    routeLines: FulfillmentPickTaskLine[]
  }
}

export interface FulfillmentPickTaskOrderDetail {
  taskOrder: FulfillmentPickTaskOrder
  fulfillmentOrder: FulfillmentOrder
  routeLines: FulfillmentPickTaskLine[]
  firstLocationCode?: string
  skuCount: number
  totalQuantity: number
  pickedQuantity: number
}

export interface FulfillmentPickTaskQuery {
  taskNo?: string
  warehouseId?: number
  taskStatus?: string
  operatorId?: number
  startTime?: string
  endTime?: string
}

export interface PlatformLabelResult {
  success: boolean
  message: string
  externalReference?: string
  labelUrl?: string
  labelBarcode?: string
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
  logisticsProductId?: number
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
