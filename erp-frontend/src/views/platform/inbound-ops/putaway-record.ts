export type PutawayQuality = 'GOOD' | 'DAMAGED'

export interface PutawayRecordFormLine {
  key?: string
  skuCode: string
  quality: PutawayQuality
  locationId?: number
  quantity?: number
  capacityOverrideReason?: string
}

export interface CapacityProjection {
  calculable: boolean
  utilizationPercent?: number
  occupiedVolumeMm3?: number
  occupiedWeightGrams?: number
  volumeAllowed?: boolean
  weightAllowed?: boolean
  skuKindsAllowed?: boolean
}

export interface CapacityLocationInput {
  locationId: number
  capacityCalculable: boolean
  capacityVolumeMm3?: number
  occupiedVolumeMm3?: number
  occupiedWeightGrams?: number
  maxWeightGrams?: number
  skuKindCount?: number
  maxSkuKinds?: number
}

export interface CapacitySkuInput {
  skuCode: string
  outerLengthMm?: number
  outerWidthMm?: number
  outerHeightMm?: number
  outerGrossWeightG?: number
}

let rowSequence = 1

export function createInitialRow(skuCode: string): PutawayRecordFormLine {
  return {
    key: `record-row-${rowSequence++}`,
    skuCode,
    quality: 'GOOD',
    locationId: undefined,
    quantity: undefined
  }
}

export function allocatedQuantity(lines: PutawayRecordFormLine[], skuCode: string) {
  return lines
    .filter(line => line.skuCode === skuCode)
    .reduce((sum, line) => sum + Number(line.quantity || 0), 0)
}

export function mergeRecordLines(lines: PutawayRecordFormLine[]) {
  const merged = new Map<string, PutawayRecordFormLine>()
  lines.forEach(source => {
    if (!source.locationId || !source.quantity) return
    const quality = source.quality || 'GOOD'
    const key = `${source.locationId}|${source.skuCode}|${quality}`
    const target = merged.get(key)
    if (!target) {
      merged.set(key, { ...source, quality })
      return
    }
    target.quantity = Number(target.quantity || 0) + source.quantity
    if (!target.capacityOverrideReason?.trim()) {
      target.capacityOverrideReason = source.capacityOverrideReason
    }
  })
  return [...merged.values()]
}

export function capacityState(value: CapacityProjection) {
  if (!value.calculable || value.utilizationPercent == null) return 'unknown' as const
  if (value.utilizationPercent > 100) return 'overflow' as const
  if (value.utilizationPercent >= 80) return 'warning' as const
  return 'normal' as const
}

export function requiresCapacityReason(value: CapacityProjection) {
  return value.calculable && (value.volumeAllowed === false || value.weightAllowed === false)
}

export function projectLocationCapacity(
  location: CapacityLocationInput,
  lines: PutawayRecordFormLine[],
  skuByCode: Map<string, CapacitySkuInput>
): CapacityProjection {
  if (!location.capacityCalculable || !location.capacityVolumeMm3) return { calculable: false }
  let addedVolume = 0
  let addedWeight = 0
  const addedKinds = new Set<string>()
  for (const line of lines.filter(value => value.locationId === location.locationId && Number(value.quantity) > 0)) {
    const sku = skuByCode.get(line.skuCode)
    if (!sku?.outerLengthMm || !sku.outerWidthMm || !sku.outerHeightMm || !sku.outerGrossWeightG) {
      return { calculable: false }
    }
    const quantity = Number(line.quantity)
    addedVolume += sku.outerLengthMm * sku.outerWidthMm * sku.outerHeightMm * quantity
    addedWeight += sku.outerGrossWeightG * quantity
    addedKinds.add(sku.skuCode)
  }
  const occupiedVolumeMm3 = Number(location.occupiedVolumeMm3 || 0) + addedVolume
  const occupiedWeightGrams = Number(location.occupiedWeightGrams || 0) + addedWeight
  const maxWeight = Number(location.maxWeightGrams || 0)
  const maxKinds = Number(location.maxSkuKinds || 0)
  const estimatedKinds = Number(location.skuKindCount || 0) + addedKinds.size
  return {
    calculable: true,
    occupiedVolumeMm3,
    occupiedWeightGrams,
    utilizationPercent: occupiedVolumeMm3 * 100 / location.capacityVolumeMm3,
    volumeAllowed: occupiedVolumeMm3 <= location.capacityVolumeMm3,
    weightAllowed: maxWeight <= 0 || occupiedWeightGrams <= maxWeight,
    skuKindsAllowed: maxKinds <= 0 || estimatedKinds <= maxKinds
  }
}
