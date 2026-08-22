export interface SortPackageItem {
  skuCode: string
  warehouseSkuCode?: string
  skuName?: string
  qty: number
  sortedQty?: number
}

export interface SortItemRow {
  skuCode: string
  warehouseSkuCode: string
  skuName?: string
  requiredQty: number
  sortedQty: number
  remainingQty: number
}

export function buildSortItemRows(items: SortPackageItem[]): SortItemRow[] {
  return items.map(item => {
    const requiredQty = item.qty || 0
    const sortedQty = item.sortedQty || 0
    return {
      skuCode: item.skuCode,
      warehouseSkuCode: item.warehouseSkuCode || item.skuCode,
      skuName: item.skuName,
      requiredQty,
      sortedQty,
      remainingQty: Math.max(0, requiredQty - sortedQty)
    }
  })
}
