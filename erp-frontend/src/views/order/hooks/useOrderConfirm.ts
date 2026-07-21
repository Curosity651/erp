import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { doRequest } from '@/utils/axios/request.ts'
import type { BaseOrderVO } from '@/api/order/types.ts'

export interface UseOrderConfirmOptions<T extends BaseOrderVO> {
  /** 判断订单是否可确认 */
  canConfirm: (row: T) => boolean
  /** 不可确认时返回原因文字 */
  reasonOf: (row: T) => string
  /** 确认 API 调用 */
  confirmApi: (ids: number[]) => Promise<any>
  /** 确认完成后刷新表格 */
  reloadTable: () => void
  /**
   * 可选：自定义确认成功处理（Yandex 需要逐条检查响应）
   * 若不传，则使用 doRequest 标准流程
   */
  onConfirmSuccess?: (res: any) => void
}

/**
 * 订单确认发货 composable（三平台通用）
 */
export function useOrderConfirm<T extends BaseOrderVO>(
  options: UseOrderConfirmOptions<T>
) {
  const confirmModal = reactive({
    open: false,
    loading: false,
    total: 0,
    eligible: [] as T[],
    ineligible: [] as (T & { reason: string })[]
  })

  function openConfirmDialog(rows: T[]) {
    const list = Array.isArray(rows) ? rows : []
    const eligible: T[] = []
    const ineligible: (T & { reason: string })[] = []

    for (const r of list) {
      if (options.canConfirm(r)) {
        eligible.push(r)
      } else {
        ineligible.push({ ...r, reason: options.reasonOf(r) })
      }
    }

    confirmModal.total = list.length
    confirmModal.eligible = eligible
    confirmModal.ineligible = ineligible
    confirmModal.open = true
  }

  async function submitConfirm() {
    if (!confirmModal.eligible.length) {
      confirmModal.open = false
      return
    }

    const ids = confirmModal.eligible.map(x => x.id)
    confirmModal.loading = true

    if (options.onConfirmSuccess) {
      // Yandex 模式：直接 await，自定义响应处理
      try {
        const res = await options.confirmApi(ids)
        options.onConfirmSuccess(res)
        confirmModal.open = false
        options.reloadTable()
      } catch (e: any) {
        message.error(e?.message || '确认发货失败')
      } finally {
        confirmModal.loading = false
      }
    } else {
      // WB/Ozon 模式：doRequest 处理
      // 注意：doRequest 不返回 Promise，不能 await。loading 重置放 onFinally；
      // 关窗与刷新只在成功时执行，失败保留弹窗以便重试
      doRequest(options.confirmApi(ids), {
        successMessage: '确认任务已提交',
        onSuccess: () => {
          confirmModal.open = false
          options.reloadTable()
        },
        onFinally: () => {
          confirmModal.loading = false
        }
      })
    }
  }

  return { confirmModal, openConfirmDialog, submitConfirm }
}
