import type { LocationInventoryGrid } from '@/api/wms/location-inventory/types'

export const filterLocationInventory = (
  rows: LocationInventoryGrid[],
  skuKeyword: string,
  zoneId?: number
) => {
  const keyword = skuKeyword.trim().toLowerCase()
  return rows.filter(row => {
    const skuMatches =
      !keyword || (row.skuCodes || []).some(skuCode => skuCode.toLowerCase().includes(keyword))
    return skuMatches && (!zoneId || row.zoneId === zoneId)
  })
}

export const buildZoneOptions = (rows: LocationInventoryGrid[]) => {
  const zones = new Map<number, string>()
  rows.forEach(row => {
    if (row.zoneId && !zones.has(row.zoneId)) {
      zones.set(row.zoneId, row.zoneName || row.zoneType || `分区 ${row.zoneId}`)
    }
  })
  return Array.from(zones, ([value, label]) => ({ value, label }))
}
