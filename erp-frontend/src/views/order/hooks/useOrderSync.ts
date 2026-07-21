import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { doRequest } from '@/utils/axios/request.ts'
import type { BaseOrderVO } from '@/api/order/types.ts'

export interface UseOrderSyncOptions {
  /** 平台标识（小写），如 'wildberries', 'ozon', 'yandex' */
  platform: string
  /** WB/Ozon: 按 ID 批量同步 */
  syncApi?: (ids: number[]) => Promise<any>
  /** Yandex: 增量同步（无 ID 参数） */
  syncIncrementalApi?: (fetchAll?: boolean) => Promise<any>
  /** 全量同步 API */
  syncAllApi: () => Promise<any>
  /** 同步完成后刷新表格 */
  reloadTable: () => void
}

/**
 * 订单同步 composable（三平台通用）
 */
export function useOrderSync(options: UseOrderSyncOptions) {
  const syncingIds = ref<Set<number>>(new Set())

  /**
   * WB/Ozon: 按 ID 批量同步
   */
  function onSync<T extends BaseOrderVO>(rowsOrRecord: T[] | T) {
    if (!options.syncApi) return

    const rows = Array.isArray(rowsOrRecord) ? rowsOrRecord : [rowsOrRecord]
    const ids = rows
      .filter(r => (r?.platform || '').toLowerCase() === options.platform)
      .map(r => r.id)

    if (!ids.length) return

    // 过滤掉正在同步的订单
    const validIds = ids.filter(id => !syncingIds.value.has(id))
    if (!validIds.length) {
      message.warning('选中订单正在同步中，请稍候')
      return
    }

    // 标记为同步中
    validIds.forEach(id => syncingIds.value.add(id))

    const isBatch = validIds.length > 1
    const loadingMsg = isBatch ? `正在同步 ${validIds.length} 个订单...` : '正在同步...'
    const hide = message.loading(loadingMsg, 0)

    doRequest(options.syncApi!(validIds), {
      successMessage: isBatch ? `${validIds.length} 个订单同步完成` : '同步完成',
      onFinally: () => {
        hide()
        validIds.forEach(id => syncingIds.value.delete(id))
        options.reloadTable()
      }
    })
  }

  /**
   * Yandex: 增量同步（无 ID 参数）
   */
  function onSyncIncremental() {
    if (!options.syncIncrementalApi) return

    const hide = message.loading('正在同步...', 0)
    doRequest(options.syncIncrementalApi!(false), {
      successMessage: '同步完成',
      onFinally: () => {
        hide()
        options.reloadTable()
      }
    })
  }

  /**
   * 三平台通用: 全量同步
   */
  function handleSyncAllOrders() {
    const hide = message.loading('正在同步全量订单...', 0)
    doRequest(options.syncAllApi(), {
      successMessage: '全量订单同步完成',
      onFinally: () => {
        hide()
        options.reloadTable()
      }
    })
  }

  return { syncingIds, onSync, onSyncIncremental, handleSyncAllOrders }
}
