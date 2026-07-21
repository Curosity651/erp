/**
 * 下架(拣货) mock 数据 + 内存态（支持下架流转、FIFO 预览、拣货单）。
 * 后端出库执行接口就绪后可整体删除（见 ./index 的 USE_MOCK 开关）。
 */
import dayjs from 'dayjs'
import type {
  OutboundOrderVO,
  OutboundOrderItemVO,
  OutboundPickingQO,
  PickAllocationVO,
  PickDTO,
  PickListVO,
  PickerVO,
  PickMode
} from './types'

function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

// mock 拣货员
const PICKERS: PickerVO[] = [
  { id: 9001, name: '张三' },
  { id: 9002, name: '李四' },
  { id: 9003, name: '王五' }
]

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
  { skuCode: 'SKU-A100', skuName: '无线鼠标' },
  { skuCode: 'SKU-B200', skuName: '机械键盘' },
  { skuCode: 'SKU-C300', skuName: '蓝牙耳机' },
  { skuCode: 'SKU-D400', skuName: 'USB-C 数据线' }
]

// 造出库单明细（允许构造缺货场景）
function buildItems(kinds: number, allowShortage: boolean): OutboundOrderItemVO[] {
  const picks = SKUS.slice(0, kinds)
  return picks.map((s, idx) => {
    const requiredQty = randInt(1, 6)
    // 第一个 SKU 在 allowShortage 时制造缺货
    const availableQty =
      allowShortage && idx === 0
        ? Math.max(0, requiredQty - randInt(1, 2))
        : requiredQty + randInt(0, 20)
    return {
      skuCode: s.skuCode,
      skuName: s.skuName,
      requiredQty,
      availableQty,
      shortage: availableQty < requiredQty
    }
  })
}

// ---- 内存态 ----
let SEQ = 1
const STORE: OutboundOrderVO[] = []
// 已下架订单的拣货单分配（orderId -> allocations）
const ALLOCATIONS = new Map<number, PickAllocationVO[]>()

function seed() {
  if (STORE.length) return
  for (let i = 0; i < 8; i++) {
    const owner = OWNERS[i % OWNERS.length]
    const wh = WAREHOUSES[i % WAREHOUSES.length]
    const kinds = randInt(1, 3)
    // 第 3、6 单构造缺货挂起场景
    const allowShortage = i === 2 || i === 5
    const items = buildItems(kinds, allowShortage)
    const anyShortage = items.some(it => it.shortage)
    const order: OutboundOrderVO = {
      id: SEQ++,
      outboundNo: `OB${dayjs().format('YYYYMMDD')}${String(1000 + i)}`,
      erpTenantId: owner.erpTenantId,
      ownerName: owner.ownerName,
      warehouseId: wh.warehouseId,
      warehouseName: wh.warehouseName,
      skuKinds: kinds,
      totalQty: items.reduce((s, it) => s + it.requiredQty, 0),
      status: anyShortage ? 'BACKORDER' : 'PENDING',
      createTime: dayjs().subtract(i, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      items
    }
    STORE.push(order)
  }
  // 预置一个"拣货中"订单便于查看拣货单
  const picking = STORE.find(o => o.status === 'PENDING')
  if (picking) {
    picking.status = 'PICKING'
    picking.pickMode = 'BY_ORDER'
    picking.pickerId = 9001
    picking.pickerName = '张三'
    ALLOCATIONS.set(picking.id, buildAllocations(picking))
  }
}

// 生成 FIFO 分配预览：把每个 SKU 的需求数按 inbound_date 升序拆到 1~2 个批次/库位
function buildAllocations(order: OutboundOrderVO): PickAllocationVO[] {
  const rows: PickAllocationVO[] = []
  ;(order.items || []).forEach((it, idx) => {
    if (it.shortage) return
    let remain = it.requiredQty
    const splits = remain > 3 ? 2 : 1
    for (let s = 0; s < splits && remain > 0; s++) {
      const take = s === splits - 1 ? remain : Math.ceil(remain / 2)
      remain -= take
      const date = dayjs()
        .subtract(30 - s * 7 - idx, 'day')
        .format('YYYY-MM-DD')
      rows.push({
        skuCode: it.skuCode,
        skuName: it.skuName,
        locationCode: `A${(idx % 3) + 1}-${String(randInt(1, 12)).padStart(2, '0')}`,
        batchNo: `${date}#${s + 1}`,
        inboundDate: date,
        takeQty: take
      })
    }
  })
  // 按库位排序（拣货路径）
  return rows.sort((a, b) => a.locationCode.localeCompare(b.locationCode))
}

export function mockPickers(): PickerVO[] {
  return PICKERS
}

export function mockPage(
  page: number,
  size: number,
  qo: OutboundPickingQO
): { records: OutboundOrderVO[]; total: number } {
  seed()
  let list = STORE.slice()
  if (qo.outboundNo) list = list.filter(o => o.outboundNo.includes(qo.outboundNo!))
  if (qo.status) list = list.filter(o => o.status === qo.status)
  if (qo.erpTenantId) list = list.filter(o => o.erpTenantId === qo.erpTenantId)
  if (qo.warehouseId) list = list.filter(o => o.warehouseId === qo.warehouseId)
  list.sort((a, b) => (a.createTime < b.createTime ? 1 : -1))
  const total = list.length
  const start = (page - 1) * size
  // 列表不带 items（贴近真实分页）
  const records = list.slice(start, start + size).map(o => {
    const row = { ...o }
    delete row.items
    return row
  })
  return { records, total }
}

export function mockDetail(id: number): OutboundOrderVO | undefined {
  seed()
  return STORE.find(o => o.id === id)
}

/** FIFO 分配预览（下架前展示） */
export function mockPreview(id: number): PickAllocationVO[] {
  seed()
  const order = STORE.find(o => o.id === id)
  if (!order) return []
  return buildAllocations(order)
}

/** 确认下架：缺货整单挂起；否则锁定 FIFO、生成拣货单、转 PICKING */
export function mockConfirmPick(dto: PickDTO): { ok: boolean; message: string } {
  seed()
  const order = STORE.find(o => o.id === dto.outboundOrderId)
  if (!order) return { ok: false, message: '订单不存在' }
  if (order.status !== 'PENDING') return { ok: false, message: '仅待下架订单可下架' }
  if ((order.items || []).some(it => it.shortage)) {
    order.status = 'BACKORDER'
    return { ok: false, message: '存在缺货 SKU，整单挂起（backorder），不可部分下架' }
  }
  order.status = 'PICKING'
  order.pickMode = dto.pickMode
  order.pickerId = dto.pickerId
  order.pickerName = PICKERS.find(p => p.id === dto.pickerId)?.name
  ALLOCATIONS.set(order.id, buildAllocations(order))
  return { ok: true, message: '下架成功，已生成拣货单' }
}

/** 拣货单 */
export function mockPickList(id: number): PickListVO | undefined {
  seed()
  const order = STORE.find(o => o.id === id)
  if (!order) return undefined
  const allocations = ALLOCATIONS.get(id) || buildAllocations(order)
  return {
    outboundNo: order.outboundNo,
    ownerName: order.ownerName,
    warehouseName: order.warehouseName,
    pickMode: (order.pickMode || 'BY_ORDER') as PickMode,
    pickerName: order.pickerName || '-',
    allocations
  }
}
