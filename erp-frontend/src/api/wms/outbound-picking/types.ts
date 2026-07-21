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
  // 缺货标记（availableQty < requiredQty）
  shortage: boolean
}

/** 出库单（列表 + 详情） */
export interface OutboundOrderVO {
  id: number
  outboundNo: string
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
  status: OutboundStatus
  createTime: string
  // 详情才带
  items?: OutboundOrderItemVO[]
}

/** FIFO 分配行（下架预览 / 拣货单明细） */
export interface PickAllocationVO {
  skuCode: string
  skuName?: string
  // 库位编码
  locationCode: string
  // 批次号（inbound_date + pick_order）
  batchNo: string
  inboundDate: string
  // 从该批次取货数
  takeQty: number
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
  ownerName: string
  warehouseName: string
  pickMode: PickMode
  pickerName: string
  // 按库位排序的取货清单
  allocations: PickAllocationVO[]
}

/** 拣货员选项（后端接入后取 sys_user / 仓库员工） */
export interface PickerVO {
  id: number
  name: string
}
