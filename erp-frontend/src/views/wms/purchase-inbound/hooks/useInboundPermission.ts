import { computed } from 'vue'
import type { ComputedRef } from 'vue'
import { useAuthorize } from '@/hooks/permission'
import { InboundStatus } from '@/api/wms/purchase-inbound/types'

export interface InboundPermissionResult {
  canEdit: ComputedRef<boolean>
  canSubmit: ComputedRef<boolean>
  canCancel: ComputedRef<boolean>
  canDelete: ComputedRef<boolean>
}

/**
 * 入库单权限判断 Hook（用于详情页/表单页，基于响应式状态）
 * @param orderStatus 订单状态（响应式）
 */
export function useInboundPermission(
  orderStatus: ComputedRef<string | undefined> | (() => string | undefined),
  permPrefix = 'wms:purchase-inbound'
): InboundPermissionResult {
  const { hasPermission } = useAuthorize()

  const getStatus = () => {
    return typeof orderStatus === 'function' ? orderStatus() : orderStatus.value
  }

  const isDraft = computed(() => getStatus() === InboundStatus.DRAFT)

  const canEdit = computed(() => isDraft.value && hasPermission(`${permPrefix}:edit`))
  const canSubmit = computed(() => isDraft.value && hasPermission(`${permPrefix}:edit`))
  const canCancel = computed(() => isDraft.value && hasPermission(`${permPrefix}:edit`))
  const isDraftOrCancelled = computed(() => {
    const s = getStatus()
    return s === InboundStatus.DRAFT || s === InboundStatus.CANCELLED
  })
  const canDelete = computed(() => isDraftOrCancelled.value && hasPermission(`${permPrefix}:del`))

  return {
    canEdit,
    canSubmit,
    canCancel,
    canDelete
  }
}

/**
 * 入库单权限判断（用于列表页，接收 record 参数）
 */
export function useInboundRecordPermission(permPrefix = 'wms:purchase-inbound') {
  const { hasPermission } = useAuthorize()

  const canEdit = (orderStatus: string): boolean => {
    return orderStatus === InboundStatus.DRAFT && hasPermission(`${permPrefix}:edit`)
  }

  const canSubmit = (orderStatus: string): boolean => {
    return orderStatus === InboundStatus.DRAFT && hasPermission(`${permPrefix}:edit`)
  }

  const canCancel = (orderStatus: string): boolean => {
    return orderStatus === InboundStatus.DRAFT && hasPermission(`${permPrefix}:edit`)
  }

  const canDelete = (orderStatus: string): boolean => {
    return (
      (orderStatus === InboundStatus.DRAFT || orderStatus === InboundStatus.CANCELLED) &&
      hasPermission(`${permPrefix}:del`)
    )
  }

  return {
    canEdit,
    canSubmit,
    canCancel,
    canDelete
  }
}
