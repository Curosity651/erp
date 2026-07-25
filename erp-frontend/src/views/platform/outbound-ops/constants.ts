import type { OutboundStatus, PickMode } from '@/api/wms/outbound-picking/types'

/** 出库单状态文案 */
export const OUTBOUND_STATUS_TEXT: Record<OutboundStatus, string> = {
  PENDING: '待下架',
  PICKING: '拣货中',
  BACKORDER: '缺货挂起',
  PICKED: '拣货完成',
  PACKED: '已打包',
  SHIPPED: '已发货',
  COMPLETED: '完成'
}

/** 状态徽标色（a-badge status） */
export const OUTBOUND_STATUS_BADGE: Record<OutboundStatus, string> = {
  PENDING: 'warning',
  PICKING: 'processing',
  BACKORDER: 'error',
  PICKED: 'success',
  PACKED: 'processing',
  SHIPPED: 'success',
  COMPLETED: 'success'
}

/** 状态筛选下拉（下架页只暴露相关状态；打包及之后统一为「拣货完成」） */
export const OUTBOUND_STATUS_OPTIONS: { label: string; value: OutboundStatus }[] = [
  { label: '待下架', value: 'PENDING' },
  { label: '拣货中', value: 'PICKING' },
  { label: '拣货完成', value: 'PICKED' },
  { label: '缺货挂起', value: 'BACKORDER' }
]

/** 下架模式文案 */
export const PICK_MODE_TEXT: Record<PickMode, string> = {
  SINGLE: '按单拣货',
  WAVE: '波次拣货',
  CENTRALIZED: '集中分拣',
  BY_ORDER: '按单分拣',
  SECONDARY: '二次分拣'
}

/** 下架模式说明（选择时提示适用场景） */
export const PICK_MODE_DESC: Record<PickMode, string> = {
  SINGLE: '单张订单独立拣货并核验',
  WAVE: '多张订单按库位和 SKU 合并拣货，之后按订单分货',
  CENTRALIZED: '一票一件（1 单 1 SKU），批量拣回打包台配货',
  BY_ORDER: '高货值/爆款/重货大货，按单找货逐单核验',
  SECONDARY: '一票多件（1 单 ≥2 SKU），拣到打包台后按篮子分配'
}

/** 下架模式单选项 */
export const PICK_MODE_OPTIONS = (Object.keys(PICK_MODE_TEXT) as PickMode[]).map(v => ({
  label: PICK_MODE_TEXT[v],
  value: v
}))
