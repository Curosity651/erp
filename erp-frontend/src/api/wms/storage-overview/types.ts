export interface StorageWarehouse {
  warehouseId: number
  warehouseName: string
  warehouseCode: string
  regionName: string
  rackCount: number
  allocatedLocations: number
  occupiedLocations: number
  occupancyRate: number
  onHandQty: number
}

export interface StorageOverview {
  warehouseCount: number
  rackCount: number
  allocatedLocations: number
  occupiedLocations: number
  occupancyRate: number
  onHandQty: number
  warehouses: StorageWarehouse[]
}

export interface StorageRack {
  rackNo: string
  locationCount: number
  occupiedCount: number
  occupancyRate: number
  monthlyFee: number | null
  effectiveTo: string | null
  expiringSoon: boolean
}

export interface StorageOwner {
  erpTenantId: number
  ownerName: string
  occupiedLocations: number
  onHandQty: number
  skuCount: number
  sharePct: number
}
