import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

/**
 * 区域库存汇总VO
 */
export interface RegionSummaryVO {
  regionId: number
  regionCode: string
  regionName: string
  ownWarehouseCount: number
  regionAvailable: number
  regionReserved: number
  regionInTransit: number
  regionDamaged: number
}

/**
 * 区域库存统计VO
 */
export interface RegionInventoryStatsVO {
  totalRegionAvailable: number
  totalRegionReserved: number
  totalRegionInTransit: number
  totalRegionDamaged: number
  regionCount: number
}

/**
 * 库存查询对象
 */
export interface InventoryQO {
  // 区域ID（区域下钻筛选）
  regionId?: number
  // 仓库ID
  warehouseId?: number
  // SKU编码（模糊匹配）
  skuCode?: string
	locationCode?: string
	quality?: 'GOOD' | 'DEFECTIVE'
  // 仓库类型
  warehouseType?: string
  // 库存状态
  stockStatus?: StockStatus
  // 仅显示有残品的记录
  hasDamaged?: boolean
}

/**
 * 库存状态枚举
 */
export type StockStatus = 'HAS_STOCK' | 'ZERO_STOCK' | 'NEGATIVE_STOCK'

/**
 * 仓库汇总查询条件
 */
export interface WarehouseSummaryQO {
  // 仓库类型 (OWN/FBO)
  warehouseType?: string
}

/**
 * SKU汇总查询条件
 */
export interface SkuSummaryQO {
  // 关键词搜索
  keyword?: string
  // 库存状态
  stockStatus?: StockStatus
}

/**
 * 库存分页参数
 */
export type InventoryPageParam = InventoryQO & PageParam

/**
 * 库存汇总VO
 */
export interface InventorySummaryVO {
  // SKU总数
  totalSkuCount: number
  // 仓内库存（计算字段：可用 + 占用）
  warehouseQuantity: number
  // 可用库存
  availableQuantity: number
  // 占用库存
  reservedQuantity: number
  // 在途库存
  inTransitQuantity: number
  // 残品库存
  damagedQuantity: number
  // 仓库数量
  warehouseCount: number
  // 总体积(m³)
  totalVolume: number | null
  // 缺失尺寸的SKU数量
  volumeMissingSkuCount: number
}

/**
 * 仓库展示信息
 */
export interface WarehouseDisplayVO {
  // 仓库ID
  id: number
  // 仓库名称
  warehouseName: string
  // 仓库类型
  warehouseType: string
}

/**
 * 仓库汇总VO
 */
export interface WarehouseSummaryVO {
  // 仓库ID
  warehouseId: number
  // 仓库展示信息
  warehouseDisplay: WarehouseDisplayVO
  // SKU数量
  skuCount: number
  // 仓内库存（计算字段）
  warehouseQuantity: number
  // 可用库存
  availableQuantity: number
  // 占用库存
  reservedQuantity: number
  // 在途库存
  inTransitQuantity: number
  // 残品库存
  damagedQuantity: number
  // 总体积(m³)
  totalVolume: number | null
}

/**
 * SKU汇总VO
 */
export interface SkuSummaryVO {
  // SKU编码
  skuCode: string
  // SKU展示信息
  skuBrief: SkuBriefVO
  // 仓库数量
  warehouseCount: number
  // 仓内库存（计算字段）
  warehouseQuantity: number
  // 货主持有总量（海外仓仓内 + FBO，不含在途与残品）
  totalHeldQuantity: number
  // 可用库存
  availableQuantity: number
  // 占用库存
  reservedQuantity: number
  // 在途库存
  inTransitQuantity: number
  // 残品库存
  damagedQuantity: number
  // FBO仓内库存
  fboWarehouseQuantity: number
  // 自有仓仓内库存
  ownWarehouseQuantity: number
  // 单件体积(m³)
  unitVolume: number | null
  // 总体积(m³)
  totalVolume: number | null
}

/**
 * 库存明细分页VO
 */
export interface InventoryPageVO {
  // 主键ID
  id: number
  // 仓库ID
  warehouseId: number
  // 仓库名称
  warehouseName: string
	regionId?: number
	regionName?: string
	locationId: number
	rackNo?: string
	locationCode: string
	zoneType?: string
	zoneName?: string
	quality: 'GOOD' | 'DEFECTIVE'
  // 仓库类型
  warehouseDisplay: WarehouseDisplayVO
  // SKU编码
  skuCode: string
  // SKU名称
  skuBrief?: SkuBriefVO
  // 仓内库存（计算字段）
  warehouseQuantity: number
	physicalQuantity: number
  // 可用库存
  availableQuantity: number
  // 占用库存
  reservedQuantity: number
  // 在途库存
  inTransitQuantity: number
  // 残品库存
  damagedQuantity: number
  // 同步时间
  syncTime?: string
  // 更新时间
  updateTime: string
}

/**
 * 库存流水视图对象
 */
export interface StockFlowVO {
  // 流水ID
  id: number
  // 仓库ID
  warehouseId?: number
  // 仓库名称
  warehouseName?: string
  // SKU编码
  skuCode: string
  // SKU名称
  skuName?: string
  // 流水类型
  flowType: string
  // 流水方向
  flowDirection: string
  // 变动数量
  quantity: number
  // 变动前总库存
  beforeTotal: number
  // 变动后总库存
  afterTotal: number
  // 变动前可用库存
  beforeAvailable?: number
  // 变动后可用库存
  afterAvailable?: number
  // 变动前占用库存
  beforeReserved?: number
  // 变动后占用库存
  afterReserved?: number
  // 来源单据类型
  sourceType?: string
  // 来源单据号
  sourceNo?: string
  // 备注
  remark?: string
  // 创建时间
  createTime: string
}

/**
 * 库存详情VO
 */
export interface InventoryDetailVO extends InventoryPageVO {
  // 最近流水列表
  recentFlows: StockFlowVO[]
}

/**
 * 最近流水VO (用于独立获取最近流水API)
 */
export interface RecentFlowVO {
  // 流水ID
  id: number
  // 仓库ID
  warehouseId: number
  // 仓库名称
  warehouseName: string
  // SKU编码
  skuCode: string
  // SKU名称
  skuName?: string
  // 流水类型
  flowType: string
  // 流水方向
  flowDirection: string
  // 变动数量
  quantity: number
  // 变动前总库存
  beforeTotal: number
  // 变动后总库存
  afterTotal: number
  // 来源单据号
  sourceNo?: string
  // 创建时间
  createTime: string
}
