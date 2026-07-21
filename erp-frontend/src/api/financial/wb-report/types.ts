import type { PageParam } from '@/api/types'

/**
 * 报表周期类型
 */
export type PeriodType = 'weekly' | 'daily'

/**
 * 周期类型选项（用于下拉选择）
 */
export const PERIOD_TYPE_OPTIONS = [
  { label: '周报 Weekly', value: 'weekly' as PeriodType },
  { label: '日报 Daily', value: 'daily' as PeriodType }
] as const

/**
 * 财务报表明细查询对象
 */
export interface WbReportDetailQO {
  /** 报表周期类型：weekly-周报, daily-日报 */
  periodType?: PeriodType
  /** 店铺ID */
  shopId?: number
  /** 报表日期范围 - 开始 */
  rrDtStart?: string
  /** 报表日期范围 - 结束 */
  rrDtEnd?: string
  /** 订单时间范围 - 开始 */
  orderDtStart?: string
  /** 订单时间范围 - 结束 */
  orderDtEnd?: string
  /** 销售时间范围 - 开始 */
  saleDtStart?: string
  /** 销售时间范围 - 结束 */
  saleDtEnd?: string
  /** 供应商操作名称 */
  supplierOperName?: string
  /** 报表ID */
  realizationreportId?: number
  /** 装配单ID */
  assemblyId?: string
  /** 是否仅显示未关联订单的记录 */
  unmatchedOrderOnly?: boolean
}

/**
 * 财务报表明细分页参数
 */
export type WbReportDetailPageParam = WbReportDetailQO & PageParam

/**
 * 财务报表明细视图对象
 */
export interface WbReportDetailVO {
  /** 主键ID */
  id: number
  /** 店铺ID */
  shopId: number
  /** 报表周期类型：weekly-周报, daily-日报 */
  periodType: string
  /** 店铺名称 */
  shopName?: string
  /** 报表ID */
  realizationreportId: number
  /** 报表开始日期 */
  dateFrom: string
  /** 报表结束日期 */
  dateTo: string
  /** 记录生成日期 */
  createDt: string
  /** 币种名称 */
  currencyName: string
  /** 合同编码 */
  suppliercontractCode: string
  /** 行ID（报表明细唯一标识） */
  rrdId: number
  /** 物流收货ID */
  giId: number
  /** 配送价格 */
  dlvPrc: number
  /** 固定费率开始日期 */
  fixTariffDateFrom: string
  /** 固定费率结束日期 */
  fixTariffDateTo: string
  /** 品类名称 */
  subjectName: string
  /** 商品ID（NMID） */
  nmId: number
  /** 品牌名 */
  brandName: string
  /** 品类组名称 */
  saName: string
  /** 尺寸 */
  tsName: string
  /** 条码 */
  barcode: string
  /** 操作类型（出售/退货等） */
  docTypeName: string
  /** 数量 */
  quantity: number
  /** 零售价 */
  retailPrice: number
  /** 零售额 */
  retailAmount: number
  /** 折扣百分比 */
  salePercent: number
  /** 佣金百分比 */
  commissionPercent: number
  /** 仓库名称 */
  officeName: string
  /** 供应商操作名称 */
  supplierOperName: string
  /** 下单时间 */
  orderDt: string
  /** 销售时间 */
  saleDt: string
  /** 操作日期 */
  rrDt: string
  /** 货品ID */
  shkId: number
  /** 折后价（RUB） */
  retailPriceWithdiscRub: number
  /** 配送金额 */
  deliveryAmount: number
  /** 退货金额 */
  returnAmount: number
  /** 配送金额（RUB） */
  deliveryRub: number
  /** 物流箱型 */
  giBoxTypeName: string
  /** 产品折扣 */
  productDiscountForReport: number
  /** 供应商促销金额 */
  supplierPromo: number
  /** SPP 百分比 */
  ppvzSppPrc: number
  /** 基础KWV百分比 */
  ppvzKvwPrcBase: number
  /** KWV百分比 */
  ppvzKvwPrc: number
  /** 评级加成百分比 */
  supRatingPrcUp: number
  /** 是否为KGV2模式 */
  isKgvpV2: number
  /** 销售佣金 */
  ppvzSalesCommission: number
  /** 应付金额 */
  ppvzForPay: number
  /** 奖励金额 */
  ppvzReward: number
  /** 收单手续费 */
  acquiringFee: number
  /** 收单费百分比 */
  acquiringPercent: number
  /** 支付处理方式 */
  paymentProcessing: string
  /** 收单银行 */
  acquiringBank: string
  /** VW金额 */
  ppvzVw: number
  /** VW金额（含增值税） */
  ppvzVwNds: number
  /** 仓库名称 */
  ppvzOfficeName: string
  /** 仓库ID */
  ppvzOfficeId: number
  /** 供应商ID */
  ppvzSupplierId: number
  /** 供应商名称 */
  ppvzSupplierName: string
  /** 供应商税号 */
  ppvzInn: string
  /** 报关单号 */
  declarationNumber: string
  /** 奖励类型 */
  bonusTypeName: string
  /** 贴纸ID */
  stickerId: string
  /** 站点国家 */
  siteCountry: string
  /** 是否DBS订单 */
  srvDbs: boolean
  /** 罚款金额 */
  penalty: number
  /** 附加支付 */
  additionalPayment: number
  /** 物流再计费成本 */
  rebillLogisticCost: number
  /** 物流再计费组织 */
  rebillLogisticOrg: string
  /** 仓储费 */
  storageFee: number
  /** 扣款 */
  deduction: number
  /** 验收入库 */
  acceptance: number
  /** 装配单ID */
  assemblyId: number
  /** KIZ码 */
  kiz: string
  /** 唯一订单行ID（全局唯一） */
  srid: string
  /** 报表类型 */
  reportType: number
  /** 是否法人主体 */
  isLegalEntity: boolean
  /** TRBX ID */
  trbxId: string
  /** 分期共同融资金额 */
  installmentCofinancingAmount: number
  /** WB折扣百分比 */
  wibesWbDiscountPercent: number
  /** 返现金额 */
  cashbackAmount: number
  /** 返现折扣 */
  cashbackDiscount: number
  /** 返现佣金变化 */
  cashbackCommissionChange: number
  /** 订单UID */
  orderUid: string
  /** 支付计划 */
  paymentSchedule: number
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/**
 * 财务报表同步请求（按时间范围）
 */
export interface FullSyncRequest {
  /** 报表周期类型（必填）：weekly-周报, daily-日报 */
  periodType: PeriodType
  /** 开始日期 (格式: YYYY-MM-DD)，可选，不填时由后端默认最近一年 */
  dateFrom?: string
  /** 结束日期 (格式: YYYY-MM-DD)，可选 */
  dateTo?: string
  /** 店铺ID列表，可选。为空时同步全部启用的 Wildberries 店铺 */
  shopIds?: number[]
}

/**
 * 同步任务响应
 */
export interface SyncTaskResponse {
  /** 作业ID */
  jobId: number
  /** 作业编号 */
  jobCode: string
  /** 同步类型 */
  syncType: string
  /** 待同步店铺数 */
  totalShops: number
  /** 任务ID列表 */
  taskIds: number[]
  /** 创建时间 */
  createTime: string
  /** 消息 */
  message: string
}
