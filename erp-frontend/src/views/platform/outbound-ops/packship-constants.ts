import type { PackMode, OutboundStatus } from '@/api/wms/outbound-shipping/types'

/** 打包模式文案 */
export const PACK_MODE_TEXT: Record<PackMode, string> = {
  BY_SKU: '按SKU打包',
  BY_ORDER: '按单打包',
  SECONDARY: '二次分拣',
  CARTON: '订单装箱'
}

/** 打包模式说明 */
export const PACK_MODE_DESC: Record<PackMode, string> = {
  BY_SKU: '同款 SKU 集中打包，适合一票一件',
  BY_ORDER: '按订单逐单打包核验，适合高货值/爆款',
  SECONDARY: '拣到打包台后按篮子二次分配，适合一票多件',
  CARTON: '多件装箱，适合大件/多件订单'
}

/** 打包模式单选项 */
export const PACK_MODE_OPTIONS = (Object.keys(PACK_MODE_TEXT) as PackMode[]).map(v => ({
  label: PACK_MODE_TEXT[v],
  value: v
}))

/** 打包签出页状态筛选（只暴露相关状态） */
export const PACKSHIP_STATUS_OPTIONS: { label: string; value: OutboundStatus }[] = [
  { label: '拣货中(待打包)', value: 'PICKING' },
  { label: '已打包(待签出)', value: 'PACKED' },
  { label: '已发货', value: 'SHIPPED' }
]

/** 币种符号（与应收账单一致，链路二物流费展示用） */
export const CURRENCY_SYMBOL = '₽'

export function formatMoney(v?: number | null): string {
  if (v == null) return '-'
  return `${CURRENCY_SYMBOL} ${v.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}
