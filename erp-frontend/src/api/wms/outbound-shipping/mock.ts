/**
 * 打包签出 mock 数据 + 内存态（打包/签出流转、物流费生成、渠道）。
 * 后端出库执行接口就绪后可整体删除（见 ./index 的 USE_MOCK 开关）。
 */
import dayjs from 'dayjs'
import type {
  PackShipOrderVO,
  PackShipItemVO,
  PackShipQO,
  PackDTO,
  ShipDTO,
  ShipResultVO,
  LogisticsChannelVO
} from './types'

function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

const OWNERS = [
  { erpTenantId: 1001, ownerName: '深圳跨境优选' },
  { erpTenantId: 1002, ownerName: '广州速卖通A' },
  { erpTenantId: 1003, ownerName: '义乌小商品' }
]
const WAREHOUSES = ['莫斯科中心仓', '新西伯利亚仓']
const SKUS = [
  { skuCode: 'SKU-A100', skuName: '无线鼠标', electronic: true },
  { skuCode: 'SKU-B200', skuName: '机械键盘', electronic: true },
  { skuCode: 'SKU-C300', skuName: '蓝牙耳机', electronic: true },
  { skuCode: 'SKU-D400', skuName: '棉质T恤', electronic: false }
]
const CHANNELS: LogisticsChannelVO[] = [
  { code: 'AUTO', name: '自动选择渠道' },
  { code: 'CDEK', name: 'CDEK' },
  { code: 'RUPOST', name: '俄罗斯邮政' },
  { code: 'BOXBERRY', name: 'Boxberry' }
]
const LOGISTICS_PRODUCTS = ['标准小包', '经济大包', '特货专线']

function buildItems(kinds: number): {
  items: PackShipItemVO[]
  hasDamaged: boolean
  hasElec: boolean
} {
  const picks = SKUS.slice(0, kinds)
  let hasDamaged = false
  let hasElec = false
  const items = picks.map((s, idx) => {
    // 第 2 个 SKU 偶发次品，触发签出拍照
    const quality: 'GOOD' | 'DAMAGED' = idx === 1 && Math.random() < 0.5 ? 'DAMAGED' : 'GOOD'
    if (quality === 'DAMAGED') hasDamaged = true
    if (s.electronic) hasElec = true
    return { skuCode: s.skuCode, skuName: s.skuName, qty: randInt(1, 5), quality }
  })
  return { items, hasDamaged, hasElec }
}

let SEQ = 1
const STORE: PackShipOrderVO[] = []

function seed() {
  if (STORE.length) return
  for (let i = 0; i < 8; i++) {
    const owner = OWNERS[i % OWNERS.length]
    const kinds = randInt(1, 3)
    const { items, hasDamaged, hasElec } = buildItems(kinds)
    // 前半拣货中(待打包)，后半已打包(待签出)
    const packed = i >= 4
    const order: PackShipOrderVO = {
      id: SEQ++,
      outboundNo: `OB${dayjs().format('YYYYMMDD')}${String(2000 + i)}`,
      erpTenantId: owner.erpTenantId,
      ownerName: owner.ownerName,
      warehouseName: WAREHOUSES[i % WAREHOUSES.length],
      skuKinds: kinds,
      totalQty: items.reduce((s, it) => s + it.qty, 0),
      status: packed ? 'PACKED' : 'PICKING',
      pickerName: ['张三', '李四', '王五'][i % 3],
      packMode: packed ? 'BY_ORDER' : undefined,
      packerName: packed ? '赵六' : undefined,
      logisticsProductName: LOGISTICS_PRODUCTS[i % LOGISTICS_PRODUCTS.length],
      // 次品 或 电子类 → 签出强制拍照
      needPhoto: hasDamaged || hasElec,
      createTime: dayjs().subtract(i, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      items
    }
    STORE.push(order)
  }
}

export function mockChannels(): LogisticsChannelVO[] {
  return CHANNELS
}

export function mockPage(
  page: number,
  size: number,
  qo: PackShipQO
): { records: PackShipOrderVO[]; total: number } {
  seed()
  let list = STORE.slice()
  if (qo.outboundNo) list = list.filter(o => o.outboundNo.includes(qo.outboundNo!))
  if (qo.status) list = list.filter(o => o.status === qo.status)
  if (qo.erpTenantId) list = list.filter(o => o.erpTenantId === qo.erpTenantId)
  list.sort((a, b) => (a.createTime < b.createTime ? 1 : -1))
  const total = list.length
  const start = (page - 1) * size
  const records = list.slice(start, start + size).map(o => {
    const row = { ...o }
    delete row.items
    return row
  })
  return { records, total }
}

export function mockDetail(id: number): PackShipOrderVO | undefined {
  seed()
  return STORE.find(o => o.id === id)
}

/** 打包：PICKING → PACKED（打印物流标签在此环节） */
export function mockPack(dto: PackDTO): { ok: boolean; message: string } {
  seed()
  const o = STORE.find(x => x.id === dto.outboundOrderId)
  if (!o) return { ok: false, message: '订单不存在' }
  if (o.status !== 'PICKING') return { ok: false, message: '仅拣货中订单可打包' }
  o.status = 'PACKED'
  o.packMode = dto.packMode
  o.packerName = dto.packerName
  return { ok: true, message: '打包完成，已打印物流标签' }
}

/** 签出：PACKED → SHIPPED。扣物理库存 + 释放 reserved，生成链路二物流费 */
export function mockShip(dto: ShipDTO): { ok: boolean; message: string; data?: ShipResultVO } {
  seed()
  const o = STORE.find(x => x.id === dto.outboundOrderId)
  if (!o) return { ok: false, message: '订单不存在' }
  if (o.status !== 'PACKED') return { ok: false, message: '仅已打包订单可签出' }
  if (o.needPhoto && !dto.photoCount) {
    return { ok: false, message: '该订单含次品/电子类，签出必须上传照片' }
  }
  const channelName = CHANNELS.find(c => c.code === dto.channel)?.name || dto.channel
  const shippingFee = o.logisticsProductName ? randInt(80, 600) : 0
  o.status = 'SHIPPED'
  o.channelName = channelName
  o.trackingNo = dto.trackingNo
  o.weight = dto.weight
  o.shippingFee = shippingFee
  return {
    ok: true,
    message: '签出成功，物理库存已扣减',
    data: { trackingNo: dto.trackingNo, channelName, shippingFee, billingRecordId: 9000 + o.id }
  }
}
