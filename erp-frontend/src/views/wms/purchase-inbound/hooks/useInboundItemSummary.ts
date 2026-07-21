import { computed } from 'vue'
import type { ComputedRef, Ref } from 'vue'

export interface InboundItemLike {
  expectedQuantity: number
  actualQuantity: number
  shortQuantity: number
  selected?: boolean
}

export interface InboundItemSummaryResult {
  totalExpected: ComputedRef<number>
  totalActual: ComputedRef<number>
  totalShort: ComputedRef<number>
}

/**
 * 入库明细合计计算 Hook
 * @param items 入库明细列表
 * @param filterSelected 是否只计算选中的项（默认 false）
 */
export function useInboundItemSummary<T extends InboundItemLike>(
  items: Ref<T[]> | ComputedRef<T[]>,
  filterSelected = false
): InboundItemSummaryResult {
  // 缓存过滤后的列表
  const filteredItems = computed(() => {
    const list = items.value || []
    return filterSelected ? list.filter(item => item.selected) : list
  })

  const totalExpected = computed(() => {
    return filteredItems.value.reduce((sum, item) => sum + item.expectedQuantity, 0)
  })

  const totalActual = computed(() => {
    return filteredItems.value.reduce((sum, item) => sum + item.actualQuantity, 0)
  })

  const totalShort = computed(() => {
    return filteredItems.value.reduce((sum, item) => sum + item.shortQuantity, 0)
  })

  return {
    totalExpected,
    totalActual,
    totalShort
  }
}
