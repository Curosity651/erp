/**
 * 海外仓平台 · 平台财务 · 应收账单（链路一：平台 → WMS 服务商）类型契约。
 *
 * 对齐后端表 wms_monthly_bill（月度账单头）。计费对象 = WMS 服务商。
 * 目前前端以 mock 驱动（见 ./mock），后端需按本契约实现（接口清单见 ./index 注释）。
 */

/** 账单状态机：草稿 → 已确认 →（已付款 / 争议） */
export type BillStatus = 'DRAFT' | 'CONFIRMED' | 'PAID' | 'DISPUTED'

/** 月度账单（对齐 wms_monthly_bill） */
export interface MonthlyBillVO {
  id: number
  // 账期 YYYY-MM
  billMonth: string
  // WMS 服务商
  wmsTenantId: number
  wmsTenantName: string
  // 货架租金（链路一：Σ 当月有效 rack.monthly_fee）
  rackFee: number
  // 操作费（汇总 wms_billing_record，按 wms_tenant_id）
  inboundFee: number
  outboundFee: number
  deliveryFee: number
  returnFee: number
  inspectionFee: number
  driverFee: number
  // 合计 = 货架租金 + 6 项操作费
  totalAmount: number
  // 状态
  status: BillStatus
  // 确认时间 / 付款时间（线下对账，手动标记）
  confirmedTime?: string
  paidTime?: string
  // 备注（如争议原因）
  remark?: string
}

/** 列表查询条件 */
export interface MonthlyBillQO {
  // 账期区间 YYYY-MM
  billMonthStart?: string
  billMonthEnd?: string
  // WMS 服务商
  wmsTenantId?: number
  // 状态（多选）
  statuses?: BillStatus[]
}

/** 生成 / 重算账单入参 */
export interface GenerateBillDTO {
  // 账期 YYYY-MM
  billMonth: string
  // WMS 服务商；不传=当月全部服务商
  wmsTenantId?: number
}

/** 生成 / 重算结果 */
export interface GenerateBillResultVO {
  // 本次新建的账单数
  created: number
  // 本次重算覆盖（原为草稿）的账单数
  recalculated: number
  // 因已确认/已付款被跳过的账单数
  skipped: number
}
