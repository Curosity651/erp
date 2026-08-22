export function canPrintLabel(taskOrderStatus?: string) {
  return taskOrderStatus === 'WAITING_PACK'
}

export function canOpenNextOrder(taskOrderStatus?: string) {
  return taskOrderStatus === 'COMPLETED'
}

export function failedOrderIds(failures?: Record<string, string>) {
  return Object.keys(failures || {}).map(Number).filter(Number.isFinite)
}
