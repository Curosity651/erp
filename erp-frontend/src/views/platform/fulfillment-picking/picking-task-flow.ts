export type PickingTaskStatus = 'PENDING' | 'PICKING' | 'PARTIAL_EXCEPTION' | 'COMPLETED' | 'CANCELLED'
export type PickingOrderStatus =
  | 'PENDING'
  | 'PICKING'
  | 'WAITING_LABEL'
  | 'COMPLETED'
  | 'EXCEPTION'
  | 'CANCELLED'

export function primaryTaskAction(
  status: PickingTaskStatus,
  operatorId?: number,
  currentUserId?: number
) {
  if (status === 'PENDING') return 'claim'
  if ((status === 'PICKING' || status === 'PARTIAL_EXCEPTION') && operatorId === currentUserId) {
    return 'work'
  }
  return 'view'
}

export function canReleaseTask(
  status: PickingTaskStatus,
  operatorId?: number,
  currentUserId?: number
) {
  return (
    (status === 'PICKING' || status === 'PARTIAL_EXCEPTION') && operatorId === currentUserId
  )
}

export function canExportTaskPackage(
  status: PickingTaskStatus,
  operatorId?: number,
  currentUserId?: number
) {
  return (status === 'PICKING' || status === 'COMPLETED') && operatorId === currentUserId
}

export function canSelectTaskOrder(status: PickingOrderStatus) {
  return status === 'PENDING' || status === 'PICKING' || status === 'WAITING_LABEL'
}
