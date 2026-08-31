export function canOpenSimplifiedTask(
  taskStatus: string,
  operationMode: string | undefined,
  operatorId: number | undefined,
  currentUserId: number | undefined
) {
  return taskStatus === 'PICKING' && operationMode !== 'SCAN' && operatorId === currentUserId
}

export function canCompleteSimplifiedTask(
  evidenceCount: number,
  orders: Array<{ orderStatus: string; labelReady: boolean }>
) {
  const activeOrders = orders.filter(order => order.orderStatus !== 'CANCELLED')
  return evidenceCount > 0 && activeOrders.length > 0 && activeOrders.every(
    order => order.orderStatus === 'PENDING' && order.labelReady
  )
}
