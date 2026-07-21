/**
 * 平台数据分析 mock 数据生成器。
 * 后端 /api/platform-dashboard/data 就绪后可整体删除本文件（见 ./index 的切换开关）。
 */
import dayjs from 'dayjs'
import type {
  PlatformDashboardQueryParams,
  PlatformDashboardDataVO,
  ThroughputTrendVO,
  ZoneType
} from './types'

// 随机整数 [min, max]
function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

// 枚举 [start, end] 之间的日期（含端点），上限 90 天防止极端范围
function enumerateDates(start: string, end: string): string[] {
  const s = dayjs(start)
  const e = dayjs(end)
  if (!s.isValid() || !e.isValid()) return [dayjs().format('YYYY-MM-DD')]
  const days = Math.min(Math.max(e.diff(s, 'day') + 1, 1), 90)
  return Array.from({ length: days }, (_, i) => s.add(i, 'day').format('YYYY-MM-DD'))
}

// 模拟仓库与货主名（后端接入后用真实名）
const MOCK_WAREHOUSES = [
  { warehouseId: 1, warehouseName: '莫斯科中心仓' },
  { warehouseId: 2, warehouseName: '新西伯利亚仓' },
  { warehouseId: 3, warehouseName: '叶卡捷琳堡仓' },
  { warehouseId: 4, warehouseName: '喀山中转仓' }
]

const MOCK_OWNERS = [
  '深圳跨境优选',
  '广州速卖通A',
  '义乌小商品',
  '杭州家居',
  '东莞3C数码',
  '宁波服饰',
  '厦门美妆',
  '泉州鞋业',
  '苏州箱包',
  '中山灯饰',
  '佛山家电',
  '温州汽配'
]

const ZONES: ZoneType[] = ['STANDARD', 'DEFECTIVE', 'RETURN', 'TEMP']

function buildThroughput(dates: string[]): ThroughputTrendVO {
  return {
    dates,
    inbound: dates.map(() => randInt(8, 60)),
    outbound: dates.map(() => randInt(20, 120)),
    returns: dates.map(() => randInt(0, 15))
  }
}

/** 生成一份 mock 看板数据 */
export function buildMockPlatformDashboard(
  params: PlatformDashboardQueryParams
): PlatformDashboardDataVO {
  const dates = enumerateDates(params.startDate, params.endDate)

  // 仓库容量（按传入 warehouseIds 过滤，不传=全部）
  const warehouses = MOCK_WAREHOUSES.filter(
    w => !params.warehouseIds?.length || params.warehouseIds.includes(w.warehouseId)
  )
  const byWarehouse = warehouses.map(w => {
    const total = randInt(400, 1200)
    const used = randInt(Math.floor(total * 0.3), Math.floor(total * 0.95))
    return { ...w, used, total }
  })

  // 分区占用
  const byZone = ZONES.map(zone => {
    const weight = zone === 'STANDARD' ? randInt(600, 1200) : randInt(30, 200)
    return {
      zone,
      locationCount: zone === 'STANDARD' ? randInt(300, 800) : randInt(20, 120),
      invQty: weight
    }
  })

  // 服务商排名（取前 10）
  const operators = MOCK_OWNERS.slice(0, 10).map((name, i) => ({
    wmsTenantId: 1000 + i,
    operatorName: name
  }))
  const byStock = operators
    .map(o => ({ ...o, qty: randInt(200, 5000) }))
    .sort((a, b) => b.qty - a.qty)
  const byThroughput = operators
    .map(o => ({ ...o, qty: randInt(10, 800) }))
    .sort((a, b) => b.qty - a.qty)

  const onHandQty = byStock.reduce((s: number, o) => s + o.qty, 0)

  return {
    opsOverview: {
      onHandQty,
      skuCount: randInt(300, 2000),
      ownerCount: owners.length,
      todayInboundQty: randInt(500, 4000),
      todayInboundOrders: randInt(10, 80),
      todayOutboundQty: randInt(800, 6000),
      todayOutboundOrders: randInt(30, 200),
      pending: {
        receiving: randInt(0, 25),
        putaway: randInt(0, 40),
        pickPack: randInt(0, 60),
        outbound: randInt(0, 30)
      }
    },
    capacity: { byWarehouse, byZone },
    throughput: buildThroughput(dates),
    operatorRanking: { byStock, byThroughput }
  }
}
