/**
 * Ozon 仓型判定（大仓 / 小仓）
 *
 * 业务规则：仓库名包含「大」字即为大仓（大件），否则为小仓。
 * 仓库名来源：erp_order.raw_json -> delivery_method.warehouse，由后端解析后填入 warehouseName。
 *
 * 仅大仓订单需要生成运单（Ozon act）；小仓订单打完面单和拣货单流程即结束。
 *
 * 已知边界：历史仓库「UNI Premium Big or Small（高客单大件…）」名字里含「大件」，
 * 会被判为大仓，但它实为大小件混合仓。其所属店铺已停用且存量订单均为 CANCELED
 * （进不了 READY_TO_SHIP → SHIPPED 的门槛），故当前无影响。若该仓将来重新产生订单，
 * 此规则会误判，届时需改为按 deliveryMethodId 白名单判定。
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
