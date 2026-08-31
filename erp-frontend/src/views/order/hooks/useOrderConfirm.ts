import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { BaseOrderVO } from '@/api/order/types.ts'
import { submitOrderFulfillment } from '@/api/order/fulfillment'
import { warehouseFulfillmentBlockReason } from './fulfillment-confirm'

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
export function useOrderConfirm<T extends BaseOrderVO>(options: UseOrderConfirmOptions<T>) {
  const confirmModal = reactive({
    open: false,
    loading: false,
    total: 0,
    eligible: [] as T[],
    ineligible: [] as (T & { reason: string })[],
    logisticsProductId: undefined as number | undefined,
    wmsWarehouseId: undefined as number | undefined
  })

  function openConfirmDialog(rows: T[]) {
    const list = Array.isArray(rows) ? rows : []
    const eligible: T[] = []
    const ineligible: (T & { reason: string })[] = []

    for (const r of list) {
      const fulfillmentReason = warehouseFulfillmentBlockReason(r)
      if (!fulfillmentReason && options.canConfirm(r)) {
        eligible.push(r)
      } else {
        ineligible.push({ ...r, reason: fulfillmentReason || options.reasonOf(r) })
      }
    }

    confirmModal.total = list.length
    confirmModal.eligible = eligible
    confirmModal.ineligible = ineligible
    confirmModal.logisticsProductId = undefined
    confirmModal.wmsWarehouseId = undefined
    confirmModal.open = true
  }

  async function submitConfirm() {
    if (!confirmModal.eligible.length) {
      confirmModal.open = false
      return
    }

    confirmModal.loading = true
    try {
      const results = await Promise.allSettled(
        confirmModal.eligible.map(order =>
          submitOrderFulfillment(order.id, confirmModal.wmsWarehouseId, confirmModal.logisticsProductId)
        )
      )
      const failures = results
        .map((result, index) => ({ result, order: confirmModal.eligible[index] }))
        .filter(item => item.result.status === 'rejected')
      const successCount = results.length - failures.length
      if (successCount > 0) message.success(`${successCount} 个订单已提交海外仓，状态为待下架`)
      if (failures.length > 0) {
        const summary = failures
          .slice(0, 3)
          .map(item => {
            const reason = (item.result as PromiseRejectedResult).reason
            return `${item.order.platformOrderId || item.order.id}：${reason?.message || '提交失败'}`
          })
          .join('；')
        message.error(`${failures.length} 个订单提交失败。${summary}`)
      }
      confirmModal.open = failures.length > 0
      options.reloadTable()
    } finally {
      confirmModal.loading = false
    }
  }

  return { confirmModal, openConfirmDialog, submitConfirm }
}
