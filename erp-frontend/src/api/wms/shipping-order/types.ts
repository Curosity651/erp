import type { PageParam } from '@/api/types'

/**
 * 物流单数据传输对象
 */
export interface ShippingOrderDTO {
  // 主键ID (编辑时必填)
  id?: number
  // 物流单号
  shippingNo: string
  // 物流商ID
  providerId: number
  // 目标区域ID
  targetRegionId: number
  // 发货日期
  shippingDate: string
  // 预计到货日期
  estimatedArrivalDate?: string
  // 预计运输时效(天)
  estimatedDays?: number
  // 物流方式: GRAY-灰关 / WHITE-白关
  shippingMethod: string
  // 物流线路: EAST-东线 / WEST-西线 / RAIL-铁路
  shippingRoute: string
  // 发货件数
  packageCount: number
  // 总重量(KG)
  totalWeight: number
  // 物流单价(USD/kg，灰关)
  unitPrice?: number
  // 运输费用(USD，白关)
  shippingFee?: number
  // 杂费(USD，白关)
  miscFee?: number
  // 物流总金额(USD)
  totalAmount?: number
  // 物流总金额(CNY)
  totalAmountCny?: number
  // 备注
  remark?: string
  // 货物明细列表
  items: ShippingOrderItemDTO[]
  // 付款状态: 0-未付 / 1-已付
  paymentStatus?: number
  // 付款凭证文件ID
  paymentVoucherFileId?: number
}

/**
 * 物流单明细数据传输对象
 */
export interface ShippingOrderItemDTO {
  // 采购单ID
  purchaseOrderId: number
  // 采购单明细ID
  purchaseOrderItemId: number
  // SKU ID
  skuId: number
  // SKU编码
  skuCode: string
  // 发货数量
  quantity: number
  // 备注
  remark?: string
}

/**
 * 物流单查询对象
 */
export interface ShippingOrderQO {
  // 物流单号
  shippingNo?: string
  // 物流商ID
  providerId?: number
  // 目标区域ID
  targetRegionId?: number
  // 物流单状态
  shippingStatus?: string
  // 付款状态
  paymentStatus?: number
  // 物流方式
  shippingMethod?: string
  // 物流线路
  shippingRoute?: string
  // SKU编码
  skuCode?: string
  // 发货日期起始
  shippingDateStart?: string
  // 发货日期结束
  shippingDateEnd?: string
}

/**
 * 物流单分页参数
 */
export type ShippingOrderPageParam = ShippingOrderQO & PageParam

/**
 * 区域展示信息
 */
export interface RegionDisplayVO {
  regionId: number
  regionCode: string
  regionName: string
}

/**
 * 物流单分页视图对象
 */
export interface ShippingOrderPageVO {
  // 主键ID
  id: number
  // 物流单号
  shippingNo: string
  // 物流商ID
  providerId: number
  // 物流商名称
  providerName: string
  // 目标区域ID
  targetRegionId: number
  // 目标区域信息
  targetRegion?: RegionDisplayVO
  // 发货日期
  shippingDate: string
  // 预计到货日期
  estimatedArrivalDate?: string
  // 预计时效(天)
  estimatedDays?: number
  // 物流方式
  shippingMethod: string
  // 物流线路
  shippingRoute: string
  // 发货件数
  packageCount: number
  // SKU种类数
  skuCount?: number
  // 总发货数量
  totalShippedQuantity?: number
  // 已入库数量
  totalReceivedQuantity?: number
  // Remaining quantity available for inbound order creation
  pendingInboundQuantity?: number
  // 总重量(KG)
  totalWeight?: number
  // 物流总金额(USD)
  totalAmount?: number
  // 付款状态: 0-未付 / 1-已付
  paymentStatus: number
  // 物流单状态
  shippingStatus: string
  // 创建时间
  createTime: string
}

/**
 * 物流单详情视图对象
 */
export interface ShippingOrderDetailVO extends ShippingOrderPageVO {
  // 预计运输时效(天)
  estimatedDays?: number
  // 物流单价(USD/kg，灰关)
  unitPrice?: number
  // 运输费用(USD，白关)
  shippingFee?: number
  // 杂费(USD，白关)
  miscFee?: number
  // 物流总金额(CNY)
  totalAmountCny?: number
  // 备注
  remark?: string
  // 货物明细列表
  items: ShippingOrderItemVO[]
  // 付款凭证文件ID
  paymentVoucherFileId?: number
  // 付款凭证文件名
  paymentVoucherFileName?: string
  // 付款凭证文件大小
  paymentVoucherFileSize?: number
  // 关联库存过账单ID
  stockPostingId?: number
}

/**
 * 物流单明细视图对象
 */
export interface ShippingOrderItemVO {
  // 主键ID
  id: number
  // 采购单ID
  purchaseOrderId: number
  // 采购单号
  purchaseOrderNo: string
  // 采购单明细ID
  purchaseOrderItemId: number
  // SKU编码
  skuCode: string
  // SKU简要信息
  skuBrief?: {
    mainImage?: string
    skuName?: string
    skuCode: string
  }
  // 发货数量
  quantity: number
  // 当前物流单可使用的发货数量
  availableQuantity: number
  // 已到货数量
  receivedQuantity: number
}

/**
 * 可发货采购明细查询对象
 */
export interface AvailableItemQO {
  // 采购单号
  purchaseOrderNo?: string
  // SKU编码
  skuCode?: string
  // 供应商ID
  supplierId?: number
  // 当前物流单ID（编辑时排除自身草稿占用）
  shippingOrderId?: number
}

/**
 * 可发货采购明细视图对象
 */
export interface AvailableItemVO {
  // 采购单ID
  purchaseOrderId: number
  // 采购单号
  purchaseOrderNo: string
  // 采购单明细ID
  purchaseOrderItemId: number
  // SKU编码
  skuCode: string
  // SKU简要信息
  skuBrief: {
    mainImage?: string
    skuName?: string
    skuCode: string
  }
  // 采购数量
  purchaseQuantity: number
  // 已发货数量
  shippedQuantity: number
  // 可发货数量
  availableQuantity: number
  // 供应商ID
  supplierId: number
  // 供应商名称
  supplierName: string
}

/**
 * 物流方式枚举
 */
export enum ShippingMethod {
  GRAY = 'GRAY',
  WHITE = 'WHITE'
}

/**
 * 物流方式描述映射
 */
export const ShippingMethodMap: Record<string, string> = {
  [ShippingMethod.GRAY]: '灰关',
  [ShippingMethod.WHITE]: '白关'
}

/**
 * 物流线路枚举
 */
export enum ShippingRoute {
  EAST = 'EAST',
  WEST = 'WEST',
  RAIL = 'RAIL'
}

/**
 * 物流线路描述映射
 */
export const ShippingRouteMap: Record<string, string> = {
  [ShippingRoute.EAST]: '东线',
  [ShippingRoute.WEST]: '西线',
  [ShippingRoute.RAIL]: '铁路'
}

/**
 * 物流单状态枚举
 */
export enum ShippingStatus {
  PENDING = 'PENDING',
  SHIPPED = 'SHIPPED',
  PARTIAL_ARRIVED = 'PARTIAL_ARRIVED',
  ALL_ARRIVED = 'ALL_ARRIVED',
  COMPLETED = 'COMPLETED'
}

/**
 * 物流单状态描述映射
 */
export const ShippingStatusMap: Record<string, string> = {
  [ShippingStatus.PENDING]: '待发货',
  [ShippingStatus.SHIPPED]: '已发货',
  [ShippingStatus.PARTIAL_ARRIVED]: '部分到货',
  [ShippingStatus.ALL_ARRIVED]: '全部到货',
  [ShippingStatus.COMPLETED]: '已完成'
}

/**
 * 物流单状态颜色映射
 */
export const ShippingStatusColorMap: Record<string, string> = {
  [ShippingStatus.PENDING]: 'default',
  [ShippingStatus.SHIPPED]: 'processing',
  [ShippingStatus.PARTIAL_ARRIVED]: 'warning',
  [ShippingStatus.ALL_ARRIVED]: 'success',
  [ShippingStatus.COMPLETED]: 'success'
}

/**
 * 付款状态描述映射
 */
export const PaymentStatusMap: Record<number, string> = {
  0: '未付',
  1: '已付'
}

/**
 * 物流单简要视图对象（用于关联展示）
 */
export interface ShippingOrderSimpleVO {
  // 主键ID
  id: number
  // 物流单号
  shippingNo: string
  // 物流商名称
  providerName: string
  // 发货日期
  shippingDate: string
  // 预计到货日期
  estimatedArrivalDate?: string
  // 物流方式
  shippingMethod: string
  // 物流线路
  shippingRoute: string
  // 物流单状态
  shippingStatus: string
  // 发货件数
  packageCount: number
  // 物流总金额(USD)
  totalAmount?: number
  // 付款状态: 0-未付 / 1-已付
  paymentStatus: number
  // 本采购单发货数量
  shippedQuantity: number
  // 本采购单已到货数量
  receivedQuantity: number
}

/**
 * 关联入库单视图对象
 */
export interface RelatedInboundVO {
  // 入库单ID
  id: number
  // 入库单号
  inboundNo: string
  // 入库仓库ID
  warehouseId: number
  // 入库仓库名称
  warehouseName: string
  // 入库日期
  inboundDate: string
  // 单据状态
  orderStatus: string
  // SKU数量
  skuCount: number
  // 入库总数量
  totalQuantity: number
}
