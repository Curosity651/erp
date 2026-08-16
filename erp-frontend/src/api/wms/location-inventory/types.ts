export interface LocationInventoryGrid {
  locationId: number
  warehouseId: number
  rackNo?: string
  sequenceNo?: number
  locationCode: string
  locationType?: string
  publicShared?: number
  capacityVolumeMm3: number
  usedVolumeMm3: number
  maxWeightGrams: number
  usedWeightGrams: number
  utilizationPercent: number
  skuKindCount: number
  totalQuantity: number
  reservedQuantity: number
  availableQuantity: number
  volumeExceeded: boolean
  weightExceeded: boolean
  capacityDataComplete: boolean
}

export interface LocationInventoryLine {
  inventoryId: number
  wmsTenantId: number
  wmsTenantName?: string
  erpTenantId: number
  ownerName?: string
  skuCode: string
  skuName?: string
  quality: string
  quantity: number
  reservedQuantity: number
  availableQuantity: number
  outerLengthMm?: number
  outerWidthMm?: number
  outerHeightMm?: number
  outerGrossWeightG?: number
}

export interface LocationInventoryDetail {
  location: LocationInventoryGrid
  items: LocationInventoryLine[]
}

export type LocationInventoryTree = Record<string, LocationInventoryGrid[]>
