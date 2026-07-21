import { reactive } from 'vue'
import type { BaseOrderVO } from '@/api/order/types.ts'

/**
 * 订单打印弹窗 + 打印历史弹窗 状态管理 composable（三平台通用）
 *
 * pickDialog / actDialog 为 Ozon 专用（拣货单 / 准备发运），其余平台不使用。
 */
export function useOrderPrint<T extends BaseOrderVO>() {
  const printDialog = reactive({ open: false, rows: [] as T[] })
  const historyDialog = reactive({ open: false })
  const pickDialog = reactive({ open: false, rows: [] as T[] })
  const actDialog = reactive({ open: false, rows: [] as T[] })

  function openPrintDialog(rows: T[]) {
    printDialog.rows = rows
    printDialog.open = true
  }

  function openHistory() {
    historyDialog.open = true
  }

  function openPickDialog(rows: T[]) {
    pickDialog.rows = rows
    pickDialog.open = true
  }

  function openActDialog(rows: T[]) {
    actDialog.rows = rows
    actDialog.open = true
  }

  return {
    printDialog,
    historyDialog,
    pickDialog,
    actDialog,
    openPrintDialog,
    openHistory,
    openPickDialog,
    openActDialog
  }
}
