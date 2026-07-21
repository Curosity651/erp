import { ref, computed, unref, readonly, type Ref, type ComputedRef, type MaybeRef } from 'vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import type { HTMLAttributes } from 'vue'

export interface UseTableSelectionOptions<T, K extends string | number = number> {
  /** 主键字段名或获取主键的函数 */
  rowKey: keyof T | ((record: T) => K)
  /** 禁用的 keys */
  disabledKeys?: MaybeRef<K[]>
  /** 最大选择数量 */
  max?: number
}

export interface UseTableSelectionReturn<T, K extends string | number> {
  // 状态（只读）
  selectedMap: Readonly<Ref<Map<K, T>>>
  selectedKeys: ComputedRef<K[]>
  selectedList: ComputedRef<T[]>
  selectedCount: ComputedRef<number>
  isEmpty: ComputedRef<boolean>

  // 查询
  isSelected: (key: K) => boolean
  isDisabled: (key: K) => boolean

  // 操作
  select: (record: T) => void
  deselect: (key: K) => void
  toggle: (record: T) => void
  clear: () => void

  // 给 ProTable 用
  rowSelection: ComputedRef<TableRowSelection<T>>
  customRow: (record: T) => HTMLAttributes
}

/**
 * 表格跨页选择 Hook
 *
 * 使用 Map 作为单一数据源，通过 onSelect/onSelectAll 实现跨页选择，
 * 避免了依赖 currentPageData 缓存带来的竞态问题。
 *
 * @example
 * ```ts
 * const {
 *   selectedList,
 *   selectedCount,
 *   isEmpty,
 *   deselect,
 *   clear,
 *   getRowSelection,
 *   getCustomRow,
 * } = useTableSelection<OrderVO>({
 *   rowKey: 'id',
 *   disabledKeys: existingOrderIds,
 * })
 * ```
 */
export function useTableSelection<T, K extends string | number = number>(
  options: UseTableSelectionOptions<T, K>
): UseTableSelectionReturn<T, K> {
  const selectedMap = ref(new Map<K, T>()) as Ref<Map<K, T>>

  // 获取记录的 key
  const getKey = (record: T): K => {
    return typeof options.rowKey === 'function'
      ? options.rowKey(record)
      : (record[options.rowKey] as K)
  }

  // 派生状态
  const selectedKeys = computed(() => [...selectedMap.value.keys()])
  const selectedList = computed(() => [...selectedMap.value.values()])
  const selectedCount = computed(() => selectedMap.value.size)
  const isEmpty = computed(() => selectedMap.value.size === 0)

  // 查询方法
  const isSelected = (key: K): boolean => selectedMap.value.has(key)

  const isDisabled = (key: K): boolean => {
    const keys = unref(options.disabledKeys)
    return keys?.includes(key) ?? false
  }

  // 操作方法
  const select = (record: T): void => {
    const key = getKey(record)
    if (isDisabled(key)) return
    if (options.max && selectedMap.value.size >= options.max) return
    // 创建新 Map 触发响应式更新
    const newMap = new Map(selectedMap.value)
    newMap.set(key, record)
    selectedMap.value = newMap
  }

  const deselect = (key: K): void => {
    if (!selectedMap.value.has(key)) return
    const newMap = new Map(selectedMap.value)
    newMap.delete(key)
    selectedMap.value = newMap
  }

  const toggle = (record: T): void => {
    const key = getKey(record)
    if (isSelected(key)) {
      deselect(key)
    } else {
      select(record)
    }
  }

  const clear = (): void => {
    selectedMap.value = new Map()
  }

  // 给 ProTable 的 rowSelection 配置（使用 computed 避免不必要的重渲染）
  const rowSelection = computed<TableRowSelection<T>>(() => ({
    selectedRowKeys: selectedKeys.value,
    onSelect: (record: T, selected: boolean) => {
      if (selected) {
        select(record)
      } else {
        deselect(getKey(record))
      }
    },
    onSelectAll: (selected: boolean, _selectedRows: T[], changeRows: T[]) => {
      changeRows.forEach(record => {
        if (selected) {
          select(record)
        } else {
          deselect(getKey(record))
        }
      })
    },
    getCheckboxProps: (record: T) => ({
      disabled: isDisabled(getKey(record))
    })
  }))

  // 点击行选中的 customRow 配置
  const customRow = (record: T): HTMLAttributes => ({
    onClick: () => {
      if (!isDisabled(getKey(record))) {
        toggle(record)
      }
    },
    style: { cursor: isDisabled(getKey(record)) ? 'not-allowed' : 'pointer' }
  })

  return {
    selectedMap: readonly(selectedMap) as Readonly<Ref<Map<K, T>>>,
    selectedKeys,
    selectedList,
    selectedCount,
    isEmpty,
    isSelected,
    isDisabled,
    select,
    deselect,
    toggle,
    clear,
    rowSelection,
    customRow
  }
}
