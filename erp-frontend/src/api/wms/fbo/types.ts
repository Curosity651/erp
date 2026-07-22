import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

export interface FboInventoryQO {
  platform?: string
  shopId?: number
  skuCode?: string
  platformWarehouseName?: string
}

export type FboInventoryPageParam = FboInventoryQO & PageParam

export interface FboInventoryPageVO {
  id: number
  platform: string
  shopId: number
  shopName: string
  platformWarehouseId: string
  platformWarehouseName: string
  platformItemId: string
  skuCode: string
  skuBrief?: SkuBriefVO
  quantity: number
  syncedAt: string
  stale: boolean
}

export interface FboInventorySummaryVO {
  skuCount: number
  shopCount: number
  totalQuantity: number
  lastSyncedAt?: string
  stale: boolean
}

/** 同步结果 */
export interface FboSyncResultVO {
  logNo: string
  totalCount: number
  successCount: number
  failCount: number
  unmappedCount: number
  syncStatus: string
  autoCreatedWarehouseCount: number
}

/** 同步日志查询条件 */
export interface FboSyncLogQO {
  shopId?: number
  syncType?: string
  syncStatus?: string
  syncTimeStart?: string
  syncTimeEnd?: string
}

/** 同步日志分页参数 */
export type FboSyncLogPageParam = FboSyncLogQO & PageParam

/** 同步日志分页项 */
export interface FboSyncLogPageVO {
  id: number
  logNo: string
  platform: string
  shopId: number
  shopName: string
  syncType: string
  totalCount: number
  successCount: number
  failCount: number
  unmappedCount: number
  syncStatus: string
  syncTime: string
  duration: number
  createTime: string
}

/** 失败明细 */
export interface FboSyncFailDetail {
  platformItemId: string
  failReason: string
  message: string
}

/** 自动创建的仓库 */
export interface AutoCreatedWarehouse {
  warehouseCode: string
  warehouseName: string
  platformWarehouseId: string
}

/** 同步日志详情 */
export interface FboSyncLogDetailVO extends FboSyncLogPageVO {
  errorMessage: string | null
  failDetails: FboSyncFailDetail[] | null
  autoCreatedWarehouseCount: number
  autoCreatedWarehouses: AutoCreatedWarehouse[] | null
}
