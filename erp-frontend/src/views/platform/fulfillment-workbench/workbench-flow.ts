export function failedOrderIds(failures?: Record<string, string>) {
  return Object.keys(failures || {}).map(Number).filter(Number.isFinite)
}

export function isShelfOrderSelectable(fulfillmentStatus?: string) {
  return fulfillmentStatus === 'WAITING_SHELF'
}
