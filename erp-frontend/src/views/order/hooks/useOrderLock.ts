import { doRequest } from '@/utils/axios/request.ts'

export interface UseOrderLockOptions {
  lockApi: (id: number) => Promise<any>
  unlockApi: (id: number) => Promise<any>
  reloadTable: () => void
}

/**
 * 订单锁定/解锁 composable（三平台通用）
 */
export function useOrderLock(options: UseOrderLockOptions) {
  const onLock = (id: number) => {
    doRequest(options.lockApi(id), {
      successMessage: '锁定成功',
      onFinally: () => options.reloadTable()
    })
  }

  const onUnlock = (id: number) => {
    doRequest(options.unlockApi(id), {
      successMessage: '解锁成功',
      onFinally: () => options.reloadTable()
    })
  }

  return { onLock, onUnlock }
}
