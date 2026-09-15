/**
 * WMS 模块共享常量
 * 统一定义库存相关的常量和工具函数，消除重复代码
 */

// ==================== 库存变更方向 ====================

/** 库存变更方向 */
export const StockDirection = {
  IN: 'IN',
  OUT: 'OUT'
} as const

export const StockDirectionLabels: Record<string, string> = {
  [StockDirection.IN]: '入库',
  [StockDirection.OUT]: '出库'
}

/** 获取方向颜色 */
export function getDirectionColor(direction: string): string {
  if (direction === StockDirection.IN) return 'success'
  if (direction === StockDirection.OUT) return 'error'
  return 'default'
}

/** 获取变动样式类名 */
export function getChangeClass(direction: string): string {
  if (direction === StockDirection.IN) return 'text-success'
  if (direction === StockDirection.OUT) return 'text-danger'
  return ''
}

// ==================== 仓库类型 ====================

/** 仓库类型映射 */
export const WarehouseTypeMap: Record<string, string> = {
  OWN: '自有仓',
  FBO: 'FBO仓',
  FBS: 'FBS仓',
  THIRD_PARTY: '第三方仓'
}

/** 仓库类型选项（用于下拉框） */
export const warehouseTypeOptions = [
  { label: '自有仓', value: 'OWN' },
  { label: 'FBO仓', value: 'FBO' }
]

/** 获取仓库类型颜色 */
export function getWarehouseTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    OWN: 'blue',
    FBO: 'green',
    FBS: 'orange',
    THIRD_PARTY: 'purple'
  }
  return colorMap[type] || 'default'
}

// ==================== 库存桶 ====================

/** 库存桶类型映射 */
export const BucketMap: Record<string, string> = {
  AVAILABLE: '可用',
  RESERVED: '占用',
  IN_TRANSIT: '在途',
  DAMAGED: '残品',
  SCRAP: '报废'
}

/** 获取库存桶文字颜色 */
export function getBucketColor(bucket: string): string {
  const colorMap: Record<string, string> = {
    AVAILABLE: '#1677ff', // 蓝色
    RESERVED: '#fa8c16', // 橙色
    IN_TRANSIT: '#722ed1', // 紫色
    DAMAGED: '#ff4d4f', // 红色
    SCRAP: '#8c8c8c' // 灰色
  }
  return colorMap[bucket] || '#595959'
}

// ==================== 库存状态 ====================

/** 库存状态选项 */
export const stockStatusOptions = [
  { label: '有库存', value: 'HAS_STOCK' },
  { label: '零库存', value: 'ZERO_STOCK' },
  { label: '负库存', value: 'NEGATIVE_STOCK' }
]

// ==================== 来源类型 ====================

/** 来源类型（统一） */
export const SourceType = {
  SHIPPING_ORDER: 'SHIPPING_ORDER',
  PURCHASE_INBOUND: 'PURCHASE_INBOUND',
  ORDER: 'ORDER',
  SALES_OUTBOUND: 'SALES_OUTBOUND',
  RETURN_INBOUND: 'RETURN_INBOUND',
  TRANSFER: 'TRANSFER',
  STOCKTAKE: 'STOCKTAKE',
  ADJUSTMENT: 'ADJUSTMENT'
} as const

export const SourceTypeLabels: Record<string, string> = {
  [SourceType.SHIPPING_ORDER]: '物流单',
  [SourceType.PURCHASE_INBOUND]: '入库单',
  [SourceType.ORDER]: '销售订单',
  [SourceType.SALES_OUTBOUND]: '销售出库单',
  [SourceType.RETURN_INBOUND]: '退货入库单',
  [SourceType.TRANSFER]: '调拨单',
  [SourceType.STOCKTAKE]: '盘点单',
  [SourceType.ADJUSTMENT]: '调整单'
}

// ==================== 来源类型路由映射 ====================

/** 来源单据类型到路由的映射 */
export const SourceTypeRouteMap: Record<string, string> = {
  SALES_OUTBOUND: '/wms/sales-outbound',
  RETURN_INBOUND: '/wms/return-inbound',
  // 盘点/调拨已迁至「海外仓作业」(/ops) 下
  TRANSFER: '/ops/transfer-order',
  STOCKTAKE: '/ops/stocktake'
}

// ==================== 过账类型 ====================

/** 过账类型映射（与后端 PostingType 枚举对应） */
export const PostingTypeMap: Record<string, string> = {
  // 物流/采购类
  LOGISTICS_SHIP: '物流单发货',
  LOGISTICS_REDIRECT: '物流目标仓调整',
  PURCHASE_RECEIVE: '采购入库',

  // 退货类
  RETURN_RECEIVE: '退货入库',

  // 销售类
  SALES_RESERVE: '订单预占',
  SALES_RELEASE: '订单释放',
  SALES_SHIP: '销售出库',

  // 调拨类
  TRANSFER_SHIP: '调拨发出',
  TRANSFER_RECEIVE: '调拨到货',
  TRANSFER_CANCEL: '调拨撤回',

  // 盘点类
  STOCKTAKE: '盘点',

  // 调整类
  SCRAP: '报废',
  OFFLINE_SALE: '线下销售',
  OTHER_OUT: '其他出库',
  OFFLINE_PURCHASE: '线下采购',
  OTHER_IN: '其他入库',

  // 残品类
  TO_DAMAGED: '转残品',
  DAMAGE_DISPOSE: '残品消耗'
}

/** 过账类型选项（用于下拉框） */
export const postingTypeOptions = Object.entries(PostingTypeMap).map(([value, label]) => ({
  value,
  label
}))

/** 获取过账类型 Tag 颜色 */
export function getPostingTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    // 物流/采购
    LOGISTICS_SHIP: 'blue',
    LOGISTICS_REDIRECT: 'geekblue',
    PURCHASE_RECEIVE: 'green',

    // 退货
    RETURN_RECEIVE: 'lime',

    // 销售
    SALES_RESERVE: 'orange',
    SALES_RELEASE: 'cyan',
    SALES_SHIP: 'red',

    // 调拨
    TRANSFER_SHIP: 'volcano',
    TRANSFER_RECEIVE: 'cyan',
    TRANSFER_CANCEL: 'default',

    // 盘点
    STOCKTAKE: 'blue',

    // 调整
    SCRAP: 'magenta',
    OFFLINE_SALE: 'volcano',
    OTHER_OUT: 'orange',
    OFFLINE_PURCHASE: 'lime',
    OTHER_IN: 'cyan',

    // 残品
    TO_DAMAGED: 'gold',
    DAMAGE_DISPOSE: 'red'
  }
  return colorMap[type] || 'default'
}
