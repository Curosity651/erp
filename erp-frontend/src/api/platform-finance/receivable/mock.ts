/**
 * 应收账单 mock 数据 + 内存态（支持状态流转 / 生成重算，贴近真实交互）。
 * 后端 wms_monthly_bill 接口就绪后，本文件可整体删除（见 ./index 的 USE_MOCK 开关）。
 */
import dayjs from 'dayjs'
import type {
  MonthlyBillVO,
  MonthlyBillQO,
  BillStatus,
  GenerateBillDTO,
  GenerateBillResultVO
} from './types'

function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

// 模拟 WMS 服务商（后端接入后用真实 tenant）
const MOCK_OPERATORS = [
  { wmsTenantId: 2001, wmsTenantName: '环球仓储服务' },
  { wmsTenantId: 2002, wmsTenantName: '中俄物流服务商' },
  { wmsTenantId: 2003, wmsTenantName: '丝路仓配' }
]

// 操作费 6 项
const OP_FEE_KEYS = [
  'inboundFee',
  'outboundFee',
  'deliveryFee',
  'returnFee',
  'inspectionFee',
  'driverFee'
] as const

function computeTotal(b: MonthlyBillVO): number {
  return b.rackFee + OP_FEE_KEYS.reduce((s, k) => s + (b[k] as number), 0)
}

// 造一张账单（金额随机）
function buildBill(
  id: number,
  billMonth: string,
  op: (typeof MOCK_OPERATORS)[number]
): MonthlyBillVO {
  const bill: MonthlyBillVO = {
    id,
    billMonth,
    wmsTenantId: op.wmsTenantId,
    wmsTenantName: op.wmsTenantName,
    rackFee: randInt(4, 20) * 1000,
    inboundFee: randInt(200, 3000),
    outboundFee: randInt(500, 5000),
    deliveryFee: randInt(300, 4000),
    returnFee: randInt(0, 1200),
    inspectionFee: randInt(0, 800),
    driverFee: randInt(0, 2000),
    totalAmount: 0,
    status: 'DRAFT'
  }
  bill.totalAmount = computeTotal(bill)
  return bill
}

// ---- 内存态：种子数据（最近 3 个月 × 3 服务商）----
let SEQ = 1
const STORE: MonthlyBillVO[] = []

function seed() {
  if (STORE.length) return
  const months = [2, 1, 0].map(i => dayjs().subtract(i, 'month').format('YYYY-MM'))
  months.forEach((m, mi) => {
    MOCK_OPERATORS.forEach(op => {
      const bill = buildBill(SEQ++, m, op)
      // 越早的账期越可能已结算
      if (mi === 0) {
        bill.status = 'PAID'
        bill.confirmedTime = dayjs(m + '-05').format('YYYY-MM-DD HH:mm:ss')
        bill.paidTime = dayjs(m + '-12').format('YYYY-MM-DD HH:mm:ss')
      } else if (mi === 1) {
        bill.status = randInt(0, 3) === 0 ? 'DISPUTED' : 'CONFIRMED'
        bill.confirmedTime = dayjs(m + '-05').format('YYYY-MM-DD HH:mm:ss')
        if (bill.status === 'DISPUTED') bill.remark = '服务商对配送费有异议，待复核'
      } else {
        bill.status = 'DRAFT'
      }
      STORE.push(bill)
    })
  })
}

function nowStr(): string {
  return dayjs().format('YYYY-MM-DD HH:mm:ss')
}

/** 分页 + 过滤 */
export function mockPageBills(
  page: number,
  size: number,
  qo: MonthlyBillQO
): { records: MonthlyBillVO[]; total: number } {
  seed()
  let list = STORE.slice()
  if (qo.billMonthStart) list = list.filter(b => b.billMonth >= qo.billMonthStart!)
  if (qo.billMonthEnd) list = list.filter(b => b.billMonth <= qo.billMonthEnd!)
  if (qo.wmsTenantId) list = list.filter(b => b.wmsTenantId === qo.wmsTenantId)
  if (qo.statuses?.length) list = list.filter(b => qo.statuses!.includes(b.status))
  // 账期倒序 + 服务商
  list.sort((a, b) =>
    a.billMonth < b.billMonth ? 1 : a.billMonth > b.billMonth ? -1 : a.wmsTenantId - b.wmsTenantId
  )
  const total = list.length
  const start = (page - 1) * size
  return { records: list.slice(start, start + size), total }
}

/** 详情 */
export function mockGetBill(id: number): MonthlyBillVO | undefined {
  seed()
  return STORE.find(b => b.id === id)
}

/** 状态流转（仅内存态） */
export function mockChangeStatus(id: number, target: BillStatus): MonthlyBillVO | undefined {
  seed()
  const b = STORE.find(x => x.id === id)
  if (!b) return undefined
  b.status = target
  if (target === 'CONFIRMED' && !b.confirmedTime) b.confirmedTime = nowStr()
  if (target === 'PAID') b.paidTime = nowStr()
  return b
}

/** 生成 / 重算：草稿态覆盖重算，已确认/已付款跳过 */
export function mockGenerate(dto: GenerateBillDTO): GenerateBillResultVO {
  seed()
  const ops = dto.wmsTenantId
    ? MOCK_OPERATORS.filter(o => o.wmsTenantId === dto.wmsTenantId)
    : MOCK_OPERATORS
  let created = 0
  let recalculated = 0
  let skipped = 0
  ops.forEach(op => {
    const existing = STORE.find(
      b => b.billMonth === dto.billMonth && b.wmsTenantId === op.wmsTenantId
    )
    if (!existing) {
      STORE.push(buildBill(SEQ++, dto.billMonth, op))
      created++
    } else if (existing.status === 'DRAFT') {
      const fresh = buildBill(existing.id, dto.billMonth, op)
      Object.assign(existing, fresh)
      recalculated++
    } else {
      skipped++
    }
  })
  return { created, recalculated, skipped }
}
