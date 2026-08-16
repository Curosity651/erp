/** 仓库结构参数 */
export interface WarehouseStructure {
  id: number
  warehouseCode: string
  warehouseName: string
  warehouseType: string
  rackRows?: number
  rackColumns?: number
  rackNoPrefix?: string
  codePadWidth?: number
  defaultLocationType?: string
  locationGenerated?: number
  palletLevels?: number
  palletPositionsPerLevel?: number
  maxSkuKindsPerPallet?: number
  allowCrossOwnerMix?: number
  defaultPalletLengthMm?: number
  defaultPalletWidthMm?: number
  defaultPalletHeightMm?: number
  defaultPalletMaxWeightKg?: number
  defaultPalletUtilization?: number
  /** 结构是否锁定（有货占用或已分配服务商 → 禁改结构/重新生成） */
  structureLocked?: boolean
  /** 有货物占用 */
  occupied?: boolean
  /** 有货占用的库位数 */
  occupiedLocationCount?: number
  /** 已分配给服务商（当前有效） */
  assigned?: boolean
  /** 已分配的货架排数（当前有效） */
  assignedRackCount?: number
  /** 已分配的服务商名称（当前有效，去重） */
  assignedOperatorNames?: string[]
  actualPhysicalLocationCount?: number
  actualPalletSlotCount?: number
  activePalletCount?: number
  unfinishedTransferCount?: number
  inProgressStocktakeCount?: number
}

/** 仓库结构参数更新 */
export interface WarehouseStructureUpdate {
  id: number
  rackRows?: number
  rackColumns?: number
  rackNoPrefix?: string
  codePadWidth?: number
  defaultLocationType?: string
  palletLevels?: number
  palletPositionsPerLevel?: number
  maxSkuKindsPerPallet?: number
  allowCrossOwnerMix?: number
  defaultPalletLengthMm?: number
  defaultPalletWidthMm?: number
  defaultPalletHeightMm?: number
  defaultPalletMaxWeightKg?: number
  defaultPalletUtilization?: number
}

export interface WarehousePalletRuleUpdate {
  id: number
  maxSkuKindsPerPallet: number
  allowCrossOwnerMix: number
  defaultPalletLengthMm: number
  defaultPalletWidthMm: number
  defaultPalletHeightMm: number
  defaultPalletMaxWeightKg: number
  defaultPalletUtilization: number
}

/** 品质分区 */
export interface WmsZone {
  id: number
  warehouseId: number
  zoneType: string
  zoneName: string
  allocatable: number
}

/** 库位 */
export interface WmsLocation {
  id: number
  warehouseId: number
  zoneId?: number
  rackNo: string
  columnNo?: number
  locationCode: string
  locationType?: string
  pickType?: string
  /** 1=虚拟库位（收纳积压货用，服务商不可见） */
  isVirtual?: number
  lengthMm?: number
  widthMm?: number
  heightMm?: number
  maxVolumeCbm?: number
  maxWeightKg?: number
  maxSkuKinds?: number
  publicShared?: number
}

export interface LogicalLocationSave {
  warehouseId?: number
  zoneId: number
  rackNo?: string
  sequenceNo?: number
  locationCode?: string
  locationType: string
  lengthMm: number
  widthMm: number
  heightMm: number
  maxWeightKg: number
  maxSkuKinds: number
  publicShared: number
}

export interface LocationSlotLevelSummary {
  locationId: number
  levelNo: number
  totalCount: number
  occupiedCount: number
  blocked?: number
}

export interface LocationSlotSummary {
  locationId: number
  totalSlots: number
  occupiedSlots: number
  blocked?: boolean
  levels: LocationSlotLevelSummary[]
}

export interface StructureLockInfo {
  locked: boolean
  occupied: boolean
  occupiedLocationCount: number
  assigned: boolean
  assignedRackCount: number
  assignedOperatorNames: string[]
  activePalletCount: number
  unfinishedTransferCount: number
  inProgressStocktakeCount: number
}
