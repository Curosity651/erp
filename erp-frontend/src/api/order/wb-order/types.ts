// 导入共享的面单批次类型
export type {
  LabelBatchVO,
  LabelBatchPageVO,
  LabelBatchFileVO,
  LabelBatchItemVO
} from '@/api/order/label-batch'
import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'
import type { BaseOrderVO } from '@/api/order/types'

// 从共用类型文件 re-export（保持向后兼容）
export { ERP_STATUS_MAP } from '@/api/order/types'
export type { ErpStatusKey } from '@/api/order/types'

export interface ErpOrderQO {
  // 订单主键
  id?: number
  // 店铺ID
  shopId?: number
  // 平台: wildberries
  platform?: string
  // 平台订单ID (WB: orderId, Ozon: order_id)
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
  // 办公点ID (WB officeId)
  destinationWarehouseId?: string
  // 锁定
  locked?: number
  // 有面单
  hasLabel?: boolean
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
 * 订单主表分页参数
 */
export type ErpOrderPageParam = ErpOrderQO & PageParam

/**
 * Wildberries 订单分页视图对象
 * 对应后端 WbOrderPageVO
 */
export interface WbOrderPageVO extends BaseOrderVO {
  // WB 特有：shipmentId 在 WB 中是必填
  shipmentId: string
  // WB 特有仓库字段
  destinationWarehouseId?: string
  destinationWarehouseName?: string
  destinationWarehouseAddress?: string
  // WB 特有金额字段（WB totalAmount 是必填 number）
  totalAmount: number
  currencyCode: string
  /** @deprecated 迭代 7 删除，改用 items */
  skuBrief?: SkuBriefVO
}

/**
 * 订单主表分页视图对象（通用）
 */
export interface ErpOrderPageVO {
  // 订单主键
  id: number
  // 店铺ID (关联 shop.id)
  shopId: number
  // 平台: wildberries|ozon
  platform: string
  // 平台订单ID (WB: orderId, Ozon: order_id)
  platformOrderId: string
  // 发货批次/履约单号 (WB: supplyId，Ozon: posting_number)
  shipmentId: string
  // 仓库ID (平台返回)
  warehouseId: string
  // 发货目的地仓库ID
  destinationWarehouseId?: string
  // 仓库显示名
  destinationWarehouseName?: string
  // 仓库地址（WB 办公点地址）
  destinationWarehouseAddress?: string
  // 履约类型 (FBS/FBO)
  fulfillmentType: string
  // 平台主状态
  platformStatus: string
  // 平台子状态
  platformSubstatus?: string
  // ERP订单状态
  erpStatus: string
  // 转换后金额
  totalAmount: string
  // CNY 金额
  convertedAmount?: string
  // 币种 (若平台返回)
  currencyCode: string
  // 转换后的币种
  convertedCurrencyCode?: string
  // Ozon专用: 结算信息 (佣金、费用、收入)
  financialData: string
  // 面单数据
  labelBase64?: string
  // 有无面单
  hasLabel?: boolean
  // 平台订单创建时间
  platformCreatedAt: string
  // 最后一次同步时间
  syncedAt: string
  // 锁定标识：1 锁定 / 0 未锁定
  locked?: number
  // SKU 简要列表
  skuList?: SkuBriefVO[]
  // SKU 总数
  skuCount?: number
  // ERP店铺名称
  erpShopName?: string
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
}

// Wildberries 平台订单状态映射（/api/v3/orders/status）
// wbStatus: waiting | sorted | sold | canceled | canceled_by_client | declined_by_client | defect | ready_for_pickup | postponed_delivery
export const WB_PLATFORM_STATUS_MAP: Record<string, { label: string; tip: string; cls: string }> = {
  waiting: { label: '待处理', tip: 'waiting', cls: 'wb-waiting' },
  sorted: { label: '仓库已分拣', tip: 'sorted', cls: 'wb-sorted' },
  sold: { label: '已成交', tip: 'sold', cls: 'wb-sold' },
  canceled: { label: '商家取消', tip: 'canceled', cls: 'wb-canceled' },
  canceled_by_client: {
    label: '客户取消（收货时）',
    tip: 'canceled_by_client',
    cls: 'wb-canceled_by_client'
  },
  declined_by_client: {
    label: '客户下单后1小时内取消',
    tip: 'declined_by_client',
    cls: 'wb-declined_by_client'
  },
  defect: { label: '因瑕疵取消', tip: 'defect', cls: 'wb-defect' },
  ready_for_pickup: {
    label: '已到自提点，待提取',
    tip: 'ready_for_pickup',
    cls: 'wb-ready_for_pickup'
  },
  postponed_delivery: {
    label: '快递延迟配送',
    tip: 'postponed_delivery',
    cls: 'wb-postponed_delivery'
  }
}

// Wildberries 平台履约状态（supplierStatus）
// new | confirm | complete | cancel | receive | reject
export const WB_SUPPLIER_STATUS_MAP: Record<string, { label: string; tip: string; cls: string }> = {
  new: { label: '新订单', tip: 'new', cls: 'wb-s-neutral' },
  confirm: { label: '拣货中/组装中', tip: 'confirm', cls: 'wb-s-process' },
  complete: { label: '已处理', tip: 'complete (in delivery)', cls: 'wb-s-transit' },
  cancel: { label: '商家取消', tip: 'cancel', cls: 'wb-s-error' },
  receive: { label: '买家已签收', tip: 'receive', cls: 'wb-s-success' },
  reject: { label: '买家拒收', tip: 'reject', cls: 'wb-s-warning' }
}
