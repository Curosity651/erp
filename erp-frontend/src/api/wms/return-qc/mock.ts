/**
 * 退货质检 mock 数据 + 内存态（收货、逐 SKU 质检+上架流转）。
 * 后端退货执行接口就绪后可整体删除（见 ./index 的 USE_MOCK 开关）。
 */
import dayjs from 'dayjs'
import type {
  ReturnOrderVO,
  ReturnOrderItemVO,
  ReturnQO,
  ReturnReceiveDTO,
  ReturnQcDTO
} from './types'

function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

const OWNERS = [
  { erpTenantId: 1001, ownerName: '深圳跨境优选' },
  { erpTenantId: 1002, ownerName: '广州速卖通A' },
  { erpTenantId: 1003, ownerName: '义乌小商品' }
]
const WAREHOUSES = [
  { warehouseId: 1, warehouseName: '莫斯科中心仓' },
  { warehouseId: 2, warehouseName: '新西伯利亚仓' }
]
const SKUS = [
  { skuCode: 'SKU-A100', skuName: '无线鼠标', electronic: true },
  { skuCode: 'SKU-B200', skuName: '机械键盘', electronic: true },
  { skuCode: 'SKU-C300', skuName: '蓝牙耳机', electronic: true },
  { skuCode: 'SKU-D400', skuName: '棉质T恤', electronic: false }
]

function buildItems(kinds: number, received: boolean): ReturnOrderItemVO[] {
  return SKUS.slice(0, kinds).map(s => {
    const expectedQty = randInt(1, 5)
    return {
      skuCode: s.skuCode,
      skuName: s.skuName,
      electronic: s.electronic,
      expectedQty,
      receivedQty: received ? expectedQty : undefined
    }
  })
}

let SEQ = 1
const STORE: ReturnOrderVO[] = []

function seed() {
  if (STORE.length) return
  for (let i = 0; i < 8; i++) {
    const owner = OWNERS[i % OWNERS.length]
    const wh = WAREHOUSES[i % WAREHOUSES.length]
    const kinds = randInt(1, 3)
    // 前 3 单待收货，中间待质检，最后 1 单已完成
    const status = i < 3 ? 'RETURN_PENDING' : i < 7 ? 'QC_PENDING' : 'COMPLETED'
    const items = buildItems(kinds, status !== 'RETURN_PENDING')
    STORE.push({
      id: SEQ++,
      returnNo: `RT${dayjs().format('YYYYMMDD')}${String(3000 + i)}`,
      erpTenantId: owner.erpTenantId,
      ownerName: owner.ownerName,
      warehouseId: wh.warehouseId,
      warehouseName: wh.warehouseName,
      skuKinds: kinds,
      totalQty: items.reduce((s, it) => s + it.expectedQty, 0),
      status,
      createTime: dayjs().subtract(i, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      items
    })
  }
}

export function mockPage(
  page: number,
  size: number,
  qo: ReturnQO
): { records: ReturnOrderVO[]; total: number } {
  seed()
  let list = STORE.slice()
  if (qo.returnNo) list = list.filter(o => o.returnNo.includes(qo.returnNo!))
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

export function mockDetail(id: number): ReturnOrderVO | undefined {
  seed()
  return STORE.find(o => o.id === id)
}

/** 退货收货：RETURN_PENDING → QC_PENDING */
export function mockReceive(dto: ReturnReceiveDTO): { ok: boolean; message: string } {
  seed()
  const o = STORE.find(x => x.id === dto.returnOrderId)
  if (!o) return { ok: false, message: '退货单不存在' }
  if (o.status !== 'RETURN_PENDING') return { ok: false, message: '仅待收货退货单可收货' }
  dto.items.forEach(i => {
    const item = (o.items || []).find(it => it.skuCode === i.skuCode)
    if (item) item.receivedQty = i.receivedQty
  })
  o.status = 'QC_PENDING'
  return { ok: true, message: '退货收货完成，转待质检' }
}

/** 质检 + 上架：QC_PENDING → COMPLETED */
export function mockQc(dto: ReturnQcDTO): { ok: boolean; message: string } {
  seed()
  const o = STORE.find(x => x.id === dto.returnOrderId)
  if (!o) return { ok: false, message: '退货单不存在' }
  if (o.status !== 'QC_PENDING') return { ok: false, message: '仅待质检退货单可质检' }
  // 校验：FAIL 且电子类必须拍照
  for (const line of dto.lines) {
    const item = (o.items || []).find(it => it.skuCode === line.skuCode)
    if (line.qcResult === 'FAIL' && item?.electronic && !line.photoCount) {
      return { ok: false, message: `${line.skuCode} 质检失败且为电子类，必须上传质检照片` }
    }
  }
  dto.lines.forEach(line => {
    const item = (o.items || []).find(it => it.skuCode === line.skuCode)
    if (item) {
      item.qcResult = line.qcResult
      item.zone = line.zone
      item.quality = line.zone === 'DEFECTIVE' ? 'DAMAGED' : 'GOOD'
      item.locationCode = line.locationCode
      item.qcRemark = line.qcRemark
    }
  })
  o.status = 'COMPLETED'
  return { ok: true, message: '质检完成，退货货物已上架回库' }
}
