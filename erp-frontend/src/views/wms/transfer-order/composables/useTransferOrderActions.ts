import { useAuthorize } from '@/hooks/permission'
import { TransferOrderStatus } from '@/api/wms/transfer-order/types'
import type { TransferOrderPageVO } from '@/api/wms/transfer-order/types'

/**
 * 调拨单操作权限判断
 */
export function useTransferOrderActions() {
  const { hasPermission } = useAuthorize()

  /** 是否可编辑（仅草稿状态） */
  const canEdit = (record: TransferOrderPageVO): boolean => {
    return record.orderStatus === TransferOrderStatus.DRAFT
  }

  /** 是否可确认出库（仅草稿状态） */
  const canShip = (record: TransferOrderPageVO): boolean => {
    return (
      record.orderStatus === TransferOrderStatus.DRAFT && hasPermission('wms:transfer-order:ship')
    )
  }

  /** 是否可确认入库（仅在途状态） */
  const canReceive = (record: TransferOrderPageVO): boolean => {
    return (
      record.orderStatus === TransferOrderStatus.IN_TRANSIT &&
      hasPermission('wms:transfer-order:receive')
    )
  }

  /** 是否可撤回（仅在途状态） */
  const canRevoke = (record: TransferOrderPageVO): boolean => {
    return (
      record.orderStatus === TransferOrderStatus.IN_TRANSIT &&
      hasPermission('wms:transfer-order:revoke')
    )
  }

  /** 是否可取消（仅草稿状态） */
  const canCancel = (record: TransferOrderPageVO): boolean => {
    return (
      record.orderStatus === TransferOrderStatus.DRAFT && hasPermission('wms:transfer-order:cancel')
    )
  }

  /** 是否可删除（草稿或已取消状态） */
  const canDelete = (record: TransferOrderPageVO): boolean => {
    return (
      (record.orderStatus === TransferOrderStatus.DRAFT ||
        record.orderStatus === TransferOrderStatus.CANCELLED) &&
      hasPermission('wms:transfer-order:del')
    )
  }

  return {
    hasPermission,
    canEdit,
    canShip,
    canReceive,
    canRevoke,
    canCancel,
    canDelete
  }
}
