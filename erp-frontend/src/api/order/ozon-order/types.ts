import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'
import type { BaseOrderVO } from '@/api/order/types'

// 从共用类型文件导出 ERP 统一状态映射
export { ERP_STATUS_MAP } from '@/api/order/types'
export type { ErpStatusKey } from '@/api/order/types'

/**
 * Ozon 订单查询对象
 */
export interface OzonOrderQO {
  // 订单主键
  id?: number
  // 店铺ID
  shopId?: number
  // 平台：ozon
  platform?: string
  // 平台订单ID (Ozon: order_id)
  platformOrderId?: string
  // 履约类型 (FBS/FBO)
  fulfillmentType?: string
  // ERP 状态
  erpStatus?: string
  // 平台主状态
  platformStatus?: string
  // 平台子状态
  platformSubstatus?: string
  // 仓库ID (平台返回)
  warehouseId?: string
  // 锁定
  locked?: number
  // 有面单
  hasLabel?: boolean
  // 仓型：BIG=大仓(大件) / SMALL=小仓
  warehouseType?: 'BIG' | 'SMALL'
  // 关键字
  keyword?: string
  // SKU编码（ERP SKU）
  skuCode?: string
  // 创建时间范围
  createTimeStart?: string
  createTimeEnd?: string
  // 更新时间范围
  updateTimeStart?: string
  updateTimeEnd?: string
  // 同步时间范围
  syncedAtStart?: string
  syncedAtEnd?: string
  // 平台创建时间范围（订单生成时间 platformCreatedAt）
  createdAtStart?: string
  createdAtEnd?: string
}

/**
 * Ozon 订单分页参数
 */
export type OzonOrderPageParam = OzonOrderQO & PageParam

/**
 * Ozon 订单分页视图对象
 */
export interface OzonOrderPageVO extends BaseOrderVO {
  // Ozon 特有：shipmentId 在 Ozon 中是必填
  shipmentId: string
  fulfillmentType: 'FBS' | 'FBO'
  // 仓库
  destinationWarehouseId?: string
  warehouseName?: string
  // 物流（FBS 独有）
  deliveryMethodId?: number
  deliveryMethodName?: string
  tplProviderId?: number
  tplProviderName?: string
  trackingNumber?: string
  // 金额（Ozon totalAmount 是必填 number）
  totalAmount: number
  currencyCode: string
  // 时间
  inProcessAt?: string
  shipmentDate?: string
  deliveringDate?: string
  /** @deprecated 迭代 7 删除，改用 items */
  skuBrief?: SkuBriefVO
}

// 导入共享的面单批次类型
export type {
  LabelBatchVO,
  LabelBatchPageVO,
  LabelBatchFileVO,
  LabelBatchItemVO
} from '@/api/order/label-batch'

/** 订单不可执行某操作时的原因说明 */
export interface OzonOrderRejectVO {
  id: number
  platformOrderId?: string
  reason: string
}

/** 运单(act)状态：CREATING=创建中 PENDING=Ozon生成中 READY=可下载 FAILED=失败 */
export type OzonActStatus = 'CREATING' | 'PENDING' | 'READY' | 'FAILED'

/** 单份运单 */
export interface OzonActVO {
  actId: number
  shopId: number
  shopName?: string
  deliveryMethodId: number
  deliveryMethodName?: string
  warehouseName?: string
  departureDate: string
  status: OzonActStatus
  orderCount: number
  fileName?: string
  objectKey?: string
  downloadUrl?: string
  errorMsg?: string
}

/** 一次【准备发运】产生的运单批次（可能跨店铺/物流方式，含多份） */
export interface OzonActBatchVO {
  batchNo: string
  acts: OzonActVO[]
  failed: OzonOrderRejectVO[]
}

/** 单份拣货单（每个店铺一份） */
export interface OzonPickListFileVO {
  shopId: number
  shopName?: string
  orderCount: number
  skuCount: number
  fileName?: string
  objectKey?: string
  downloadUrl?: string
  errorMsg?: string
}

/** 一次【打印拣货单】产生的批次 */
export interface OzonPickListBatchVO {
  batchNo: string
  files: OzonPickListFileVO[]
  failed: OzonOrderRejectVO[]
}

/**
 * 同步摘要 VO
 */
export interface SyncSummaryVO {
  total?: number
  processed?: number
  success?: number
  skipped?: number
  failed?: number
}

/**
 * Ozon 平台状态枚举
 * 基于 Ozon Seller API 官方状态全集（与后端 OzonStatusEnum 对齐）
 */
export type OzonStatusKey =
  | 'awaiting_registration'
  | 'acceptance_in_progress'
  | 'awaiting_approve'
  | 'awaiting_packaging'
  | 'awaiting_deliver'
  | 'driver_pickup'
  | 'sent_by_seller'
  | 'delivering'
  | 'delivered'
  | 'cancelled'
  | 'not_accepted'
  | 'arbitration'
  | 'client_arbitration'

/**
 * Ozon 平台主状态映射
 * 映射 platformStatus 字段
 */
export const OZON_STATUS_MAP: Record<string, { label: string; tip: string; cls: string }> = {
  awaiting_registration: { label: '等待注册', tip: 'Awaiting Registration - 备货前过渡状态', cls: 'default' },
  acceptance_in_progress: { label: '验收中', tip: 'Acceptance In Progress - 备货前过渡状态', cls: 'default' },
  awaiting_approve: { label: '等待确认', tip: 'Awaiting Approve - 备货前过渡状态', cls: 'default' },
  awaiting_packaging: { label: '等待打包', tip: 'Awaiting Packaging - 等待商家打包', cls: 'processing' },
  awaiting_deliver: { label: '等待发货', tip: 'Awaiting Deliver - 等待商家发货', cls: 'processing' },
  driver_pickup: { label: '司机取货中', tip: 'Driver Pickup - 司机取货中', cls: 'processing' },
  sent_by_seller: { label: '卖家已发出', tip: 'Sent By Seller - 卖家已发出（跨境头程）', cls: 'processing' },
  delivering: { label: '配送中', tip: 'Delivering - 配送中', cls: 'processing' },
  delivered: { label: '已送达', tip: 'Delivered - 已送达客户', cls: 'success' },
  cancelled: { label: '已取消', tip: 'Cancelled - 订单已取消', cls: 'gray' },
  not_accepted: { label: '分拣中心拒收', tip: 'Not Accepted - 分拣中心未接受，需人工处理', cls: 'error' },
  arbitration: { label: '仲裁中', tip: 'Arbitration - 仲裁中，需人工处理', cls: 'warning' },
  client_arbitration: { label: '客户仲裁', tip: 'Client Arbitration - 快递客户仲裁，需人工处理', cls: 'warning' }
}

/**
 * Ozon 子状态映射
 * 映射 platformSubstatus 字段（数据库存储的是 Ozon API 原始 posting_* 格式）
 * 来源：Ozon Seller API - FBS/FBO posting substatus
 */
export const OZON_SUBSTATUS_MAP: Record<string, { label: string; tip: string; cls: string }> = {
  // ======== 处理阶段 ========
  posting_created: { label: '已创建', tip: '发货单已创建', cls: 'default' },
  posting_packing: { label: '打包中', tip: '正在打包', cls: 'processing' },
  posting_split_pending: { label: '拆分待处理', tip: '订单拆分待处理', cls: 'default' },
  posting_registered: { label: '已注册', tip: '发货单已注册', cls: 'processing' },
  posting_registration_error: { label: '注册错误', tip: '发货单注册错误', cls: 'error' },
  posting_awaiting_passport_data: { label: '等待护照数据', tip: '等待买家护照数据', cls: 'warning' },
  posting_acceptance_in_progress: { label: '验收中', tip: '正在进行验收', cls: 'processing' },
  posting_not_in_sort_center: { label: '未入分拣中心', tip: '未被分拣中心接收', cls: 'error' },

  // ======== 物流阶段 ========
  posting_transferring_to_delivery: { label: '转交配送中', tip: '正在转交配送服务', cls: 'processing' },
  posting_transferred_to_courier_service: { label: '已转交快递', tip: '已转交快递服务', cls: 'processing' },
  posting_in_courier_service: { label: '快递在途', tip: '快递员在路上', cls: 'processing' },
  posting_driver_pick_up: { label: '司机取件', tip: '司机正在取件', cls: 'processing' },
  posting_in_carriage: { label: '运输中', tip: '在运输途中', cls: 'processing' },
  posting_not_in_carriage: { label: '未加入运输', tip: '未被加入运输', cls: 'warning' },
  posting_on_way_to_city: { label: '运往城市', tip: '正在运往目的城市', cls: 'processing' },
  posting_on_way_to_pickup_point: { label: '运往自提点', tip: '正在运往自提点', cls: 'processing' },
  posting_in_pickup_point: { label: '在自提点', tip: '已到达自提点', cls: 'success' },

  // ======== 仲裁 ========
  posting_in_arbitration: { label: '仲裁中', tip: '订单进入仲裁流程', cls: 'error' },
  posting_in_client_arbitration: { label: '客户仲裁', tip: '客户发起配送仲裁', cls: 'error' },

  // ======== 完成 ========
  posting_delivered: { label: '已送达', tip: '已送达', cls: 'success' },
  posting_received: { label: '已收货', tip: '买家已收货', cls: 'success' },
  posting_conditionally_delivered: { label: '有条件送达', tip: '有条件送达', cls: 'success' },
  posting_returned_to_warehouse: { label: '已退回仓库', tip: '已退回仓库', cls: 'warning' },
  posting_canceled: { label: '已取消', tip: '发货单已取消', cls: 'default' },
  ship_failed: { label: '发货失败', tip: '发货失败', cls: 'error' }
}
