/**
 * Ozon 仓型判定（大仓 / 小仓）
 *
 * 业务规则：仓库名包含「大」字即为大仓（大件），否则为小仓。
 * 仓库名来源：erp_order.raw_json -> delivery_method.warehouse，由后端解析后填入 warehouseName。
 *
 * 此判定仅用于界面仓型标签；是否需要 Ozon 交接单由店铺与 deliveryMethodId 配置决定。
 */
export type WarehouseType = 'BIG' | 'SMALL'

/** 大仓关键字 */
const BIG_WAREHOUSE_KEYWORD = '大'

/** 仓库名是否为大仓 */
export function isBigWarehouse(warehouseName?: string | null): boolean {
  return !!warehouseName && warehouseName.includes(BIG_WAREHOUSE_KEYWORD)
}

/** 仓型；仓库名缺失时返回 undefined（无法判定） */
export function warehouseTypeOf(warehouseName?: string | null): WarehouseType | undefined {
  if (!warehouseName) return undefined
  return isBigWarehouse(warehouseName) ? 'BIG' : 'SMALL'
}

/** 仓型展示配置 */
export const WAREHOUSE_TYPE_META: Record<WarehouseType, { label: string; color: string }> = {
  BIG: { label: '大仓', color: 'blue' },
  SMALL: { label: '小仓', color: 'default' }
}
