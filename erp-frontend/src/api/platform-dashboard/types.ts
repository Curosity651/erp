/**
 * 海外仓平台 - 数据分析（平台数据分析 900300）类型契约。
 *
 * 说明：本模块为「仓储运营视角」的平台看板，跨全部货主聚合。
 * 目前前端以 mock 数据驱动（见 ./mock），后端需按本契约实现：
 *   POST /api/platform-dashboard/data
 *   入参 PlatformDashboardQueryParams → 返回 PlatformDashboardDataVO
 */

/** 查询入参 */
export interface PlatformDashboardQueryParams {
  // 开始日期 YYYY-MM-DD
  startDate: string
  // 结束日期 YYYY-MM-DD
  endDate: string
  // 仓库过滤（不传=全部仓库）
  warehouseIds?: number[]
  // WMS 服务商过滤（不传=全部服务商；按服务商收窄其名下货主的业务数据）
  wmsTenantIds?: number[]
}

/** 品质分区类型（与后端 wms_zone 语义一致） */
export type ZoneType = 'STANDARD' | 'DEFECTIVE' | 'RETURN' | 'TEMP'

/** A - 运营总览 KPI */
export interface OpsOverviewVO {
  // 在库总量（件数）
  onHandQty: number
  // 涉及 SKU 数
  skuCount: number
  // 涉及货主数
  ownerCount: number
  // 今日入库件数 / 单数
  todayInboundQty: number
  todayInboundOrders: number
  // 今日出库件数 / 单数
  todayOutboundQty: number
  todayOutboundOrders: number
  // 待处理作业积压（各单据数）
  pending: {
    // 待收货
    receiving: number
    // 待上架
    putaway: number
    // 待拣货打包
    pickPack: number
    // 待出库
    outbound: number
  }
}

/** B - 单仓库容占用 */
export interface WarehouseCapacityVO {
  warehouseId: number
  warehouseName: string
  // 已占用库位数
  used: number
  // 库位总数
  total: number
}

/** B - 分区占用 */
export interface ZoneOccupancyVO {
  zone: ZoneType
  // 该分区库位数
  locationCount: number
  // 该分区库存件数
  invQty: number
}

/** B - 仓容利用率 */
export interface CapacityVO {
  byWarehouse: WarehouseCapacityVO[]
  byZone: ZoneOccupancyVO[]
}

/** C - 吞吐趋势（度量：单据数） */
export interface ThroughputTrendVO {
  // 日期轴 YYYY-MM-DD
  dates: string[]
  // 入库单数（按日）
  inbound: number[]
  // 出库单数（按日）
  outbound: number[]
  // 退货单数（按日）
  returns: number[]
}

/** D - 排名单项（中性字段，供服务商维度排名卡通用） */
export interface RankingItemVO {
  // 服务商租户 id
  wmsTenantId: number
  // 服务商名称
  operatorName: string
  // 排名度量值（在库件数 / 吞吐单数，视卡片而定）
  qty: number
}

/** D - WMS 服务商排名 */
export interface OperatorRankingVO {
  // 在库占用排名（件数）
  byStock: RankingItemVO[]
  // 吞吐排名（单数）
  byThroughput: RankingItemVO[]
}

/** 平台数据分析总返回 */
export interface PlatformDashboardDataVO {
  opsOverview: OpsOverviewVO
  capacity: CapacityVO
  throughput: ThroughputTrendVO
  operatorRanking: OperatorRankingVO
}
