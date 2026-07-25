/**
 * 海外仓平台 · 海外仓作业 · 下架(拣货) 类型契约（业务需求 1.3.1）。
 *
 * 平台接收货主推送的销售/自定义出库单，执行下架：选模式+指定拣货员 → FIFO 分配 → 生成拣货单。
 * 对齐后端表 wms_outbound_order / wms_outbound_order_item / wms_physical_inventory。
 * 目前前端以 mock 驱动（见 ./mock），后端需按本契约实现（接口清单见 ./index 注释）。
 */

/** 出库单状态（下架页主要关注 PENDING 待下架 / PICKING 拣货中 / BACKORDER 缺货挂起） */
export type OutboundStatus =
  | 'PENDING' // 待下架
  | 'PICKING' // 拣货中
  | 'BACKORDER' // 缺货挂起
  | 'PICKED' // 拣货完成（下架页专用：打包/签出/完成 统一归并展示）
  | 'PACKED' // 已打包
  | 'SHIPPED' // 已发货
  | 'COMPLETED' // 完成

/** 下架模式（三选一，业务需求 1.3.1） */
export type PickMode =
  | 'SINGLE' // 按单拣货任务
  | 'WAVE' // 批量波次任务
  | 'CENTRALIZED' // 集中分拣（一票一件）
  | 'BY_ORDER' // 按单分拣（高货值/爆款/重货）
  | 'SECONDARY' // 二次分拣（一票多件）

/** 出库单明细 */
export interface OutboundOrderItemVO {
  skuCode: string
  skuName?: string
  // 需求数
  requiredQty: number
  // 可用良品库存（quality=GOOD 且 allocatable）
  availableQty: number
  // 当前订单自己已经锁定的库存
  ownReservedQty: number
  // 其他订单也不可再使用的公共剩余库存
  publicAvailableQty: number
  // 缺货标记（availableQty < requiredQty）
  shortage: boolean
}

/** 出库单（列表 + 详情） */
export interface OutboundOrderVO {
  id: number
  outboundNo: string
  sourceType: 'SALES' | 'CUSTOM'
  /** 本张销售出库单关联的电商平台订单数。 */
  salesOrderCount: number
  erpTenantId: number
  ownerName: string
  // 所属WMS服务商ID
  operatorId?: number
  // 所属WMS服务商名称
  operatorName?: string
  warehouseId: number
  warehouseName: string
  // SKU 种类数 / 总件数
  skuKinds: number
  totalQty: number
  // 下架后回填
  pickMode?: PickMode
  pickerId?: number
  pickerName?: string
  pickTaskId?: number
  pickTaskNo?: string
  pickTaskOutboundOrderCount?: number
  pickTaskSalesOrderCount?: number
  status: OutboundStatus
  createTime: string
  // 详情才带
  items?: OutboundOrderItemVO[]
}

/** FIFO 分配行（下架预览 / 拣货单明细） */
export interface PickAllocationVO {
  lineId?: number
  physicalInventoryId?: number
  skuCode: string
  skuName?: string
  // 库位编码
  locationCode: string
  // 批次号（inbound_date + pick_order）
  batchNo: string
  inboundDate: string
  // 从该批次取货数
  takeQty: number
  plannedQty?: number
  pickedQty?: number
  remainingQty?: number
  shortageQty?: number
  lineStatus?: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'EXCEPTION' | 'CANCELLED'
  exceptionReason?: string
  palletId?: number
  palletNo?: string
  slotCode?: string
  pickStrategy?: 'WHOLE_PALLET' | 'PIECE'
}

/** 列表查询条件 */
export interface OutboundPickingQO {
  outboundNo?: string
  status?: OutboundStatus
  erpTenantId?: number
  /** WMS服务商筛选 */
  wmsTenantId?: number
  warehouseId?: number
  /** 创建日期起始 YYYY-MM-DD */
  createTimeStart?: string
  /** 创建日期结束 YYYY-MM-DD */
  createTimeEnd?: string
}

/** 下架入参 */
export interface PickDTO {
  outboundOrderId: number
  pickMode: PickMode
  pickerId: number
}

/** 拣货单（PICKING 后查看） */
export interface PickListVO {
  outboundNo: string
  sourceType: 'SALES' | 'CUSTOM'
  ownerName: string
  warehouseName: string
  pickMode: PickMode
  pickerName: string
  taskId?: number
  taskNo?: string
  taskType?: 'SINGLE' | 'WAVE'
  taskStatus?: 'PICKING' | 'EXCEPTION' | 'SORTING' | 'COMPLETED' | 'CANCELLED'
  outboundOrderCount?: number
  salesOrderCount?: number
  secondaryOrderCount?: number
  completable?: boolean
  plannedQuantity?: number
  pickedQuantity?: number
  exceptionReason?: string
  outboundOrders?: PickTaskOutboundVO[]
  // 按库位排序的取货清单
  allocations: PickAllocationVO[]
}

export interface PickTaskOutboundVO {
  outboundOrderId: number
  outboundNo: string
  salesOrderCount: number
  skuCount: number
  totalQuantity: number
  toteNo?: string
  sortRequired: boolean
  packages?: OutboundPackageVO[]
}

export interface OutboundPackageVO {
  id: number
  outboundOrderId: number
  erpOrderId: number
  platformOrderId: string
  shopId: number
  sortCode?: string
  sortStatus: 'PENDING' | 'SORTED' | 'NOT_REQUIRED'
  labelStatus: 'NOT_READY' | 'READY' | 'EXTERNAL_CONFIRMED'
  handoverRequired?: boolean
  handoverStatus?: 'NOT_REQUIRED' | 'PENDING' | 'READY' | 'EXTERNAL_CONFIRMED'
  packStatus: 'PENDING' | 'PACKED'
  items: Array<{
    skuCode: string
    skuName?: string
    qty: number
    sortedQty?: number
    packedQty?: number
    quality: string
  }>
}

/** 拣货员选项（后端接入后取 sys_user / 仓库员工） */
export interface PickerVO {
  id: number
  name: string
}

export interface BatchPickPreviewDTO {
  outboundOrderIds: number[]
  maxOrdersPerTask?: number
  wholePalletPriority?: boolean
}

export interface BatchPickDTO extends BatchPickPreviewDTO {
  pickerId: number
}

export interface PickTaskPreviewVO {
  warehouseId: number
  warehouseName: string
  erpTenantId: number
  ownerName: string
  sourceType: 'SALES' | 'CUSTOM'
  taskType: 'SINGLE' | 'WAVE'
  outboundOrderIds: number[]
  /** 出库单数，不是销售订单数。 */
  orderCount: number
  salesOrderCount: number
  skuCount: number
  totalQuantity: number
  wholePalletCount: number
  secondaryOrderCount: number
}

export interface BatchPickPreviewVO {
  /** 已选择的出库单数量。 */
  selectedOrderCount: number
  selectedSalesOrderCount: number
  taskCount: number
  totalQuantity: number
  wholePalletCount: number
  secondaryOrderCount: number
  tasks: PickTaskPreviewVO[]
}

export interface BatchPickResultVO {
  taskCount: number
  /** 已纳入任务的出库单数量。 */
  orderCount: number
  salesOrderCount: number
  taskIds: number[]
  taskNos: string[]
}

export interface PickLineScanDTO {
  taskId: number
  lineId: number
  scanCode: string
  locationScanCode?: string
  quantity: number
  manual?: boolean
}

export interface PickExceptionDTO {
  taskId: number
  lineId: number
  shortageQty: number
  reason: string
}

export interface ResolvePickExceptionDTO {
  taskId: number
  action: 'RETRY' | 'REALLOCATE' | 'SHORT_CLOSE'
  replacementInventoryId?: number
  remark?: string
}

export interface PackageScanDTO {
  outboundOrderId: number
  packageId: number
  scanCode: string
  quantity: number
  manual?: boolean
}
