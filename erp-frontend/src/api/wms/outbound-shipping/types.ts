/**
 * 海外仓平台 · 海外仓作业 · 打包签出 类型契约（业务需求 1.3.2）。
 *
 * 接下架(拣货)之后：PICKING 拣货中 → 打包 → PACKED 已打包 → 签出 → SHIPPED 已发货 → COMPLETED。
 * 签出时才真正扣减物理库存 + 释放 reserved_qty，并生成链路二物流费（wms_client_billing_record）。
 * 对齐后端表 wms_outbound_order / wms_physical_inventory / wms_client_billing_record。
 * 目前前端以 mock 驱动（见 ./mock），后端需按本契约实现（接口清单见 ./index 注释）。
 */
import type { OutboundStatus } from '@/api/wms/outbound-picking/types'
import type { OutboundPackageVO } from '@/api/wms/outbound-picking/types'

export type { OutboundStatus }

/** 打包模式（四选一，业务需求 1.3.2） */
export type PackMode =
  | 'BY_SKU' // 按 SKU 打包
  | 'BY_ORDER' // 按单打包
  | 'SECONDARY' // 二次分拣
  | 'CARTON' // 订单装箱

/** 出库单明细（打包签出视角） */
export interface PackShipItemVO {
  skuCode: string
  skuName?: string
  qty: number
  sortedQty?: number
  packedQty?: number
  quality: 'GOOD' | 'DAMAGED'
}

/** 打包签出订单（列表 + 详情） */
export interface PackShipOrderVO {
  id: number
  outboundNo: string
  sourceType: 'SALES' | 'CUSTOM'
  salesOrderCount: number
  platform?: string
  documentMode?: 'WAREHOUSE_PRINT' | 'OWNER_PROVIDED'
  erpTenantId: number
  ownerName: string
  // 所属WMS服务商ID
  operatorId?: number
  // 所属WMS服务商名称
  operatorName?: string
  warehouseName: string
  skuKinds: number
  totalQty: number
  // 主要关注 PICKING / PACKED / SHIPPED / COMPLETED
  status: OutboundStatus
  pickerName?: string
  // 打包后回填
  packMode?: PackMode
  packerName?: string
  // 关联物流产品（链路二计费依据）
  logisticsProductName?: string
  // 次品/电子类 → 签出强制拍照（BR-04）
  needPhoto: boolean
  // 签出后回填
  channelName?: string
  trackingNo?: string
  weight?: number // kg
  shippingFee?: number // 链路二物流费
  createTime: string
  // 详情才带
  items?: PackShipItemVO[]
  packages?: OutboundPackageVO[]
}

/** 列表查询条件 */
export interface PackShipQO {
  outboundNo?: string
  status?: OutboundStatus
  erpTenantId?: number
  /** WMS服务商筛选 */
  wmsTenantId?: number
  /** 创建日期起始 YYYY-MM-DD */
  createTimeStart?: string
  /** 创建日期结束 YYYY-MM-DD */
  createTimeEnd?: string
}

/** 打包入参 */
export interface PackDTO {
  outboundOrderId: number
  packMode: PackMode
  packerName?: string
}

export interface PackPackageDTO {
  outboundOrderId: number
  packageId: number
  packerName?: string
}

export interface PackageScanDTO {
  outboundOrderId: number
  packageId: number
  scanCode: string
  quantity: number
  manual?: boolean
}

export interface OzonActVO {
  actId: number
  status: 'CREATING' | 'PENDING' | 'READY' | 'FAILED'
  fileName?: string
  downloadUrl?: string
  errorMsg?: string
}

export interface OzonActBatchVO {
  batchNo: string
  acts: OzonActVO[]
  failed: Array<{ orderId: number; platformOrderId: string; reason: string }>
}

/** 签出入参 */
export interface ShipDTO {
  outboundOrderId: number
  // 渠道代码；'AUTO' = 自动选择渠道
  channel: string
  trackingNo?: string
  weight: number
  // mock：上传照片数（真实为文件 id 列表）
  photoCount?: number
}

/** 签出结果 */
export interface ShipResultVO {
  trackingNo?: string
  channelName: string
  // 生成的链路二物流费
  shippingFee: number
  billingRecordId?: number
}

/** 物流渠道选项 */
export interface LogisticsChannelVO {
  code: string
  name: string
}
