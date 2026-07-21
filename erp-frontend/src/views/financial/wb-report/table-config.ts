/**
 * WB 财务报表明细表格共享配置
 * 用于 WbFinancialReportPage 和 OrphanRecordPanel 复用
 */
import type { ProColumns } from '#/table'

// ===================== 字段分类列表 =====================

/** 日期字段列表 */
export const DATE_FIELDS = [
  'dateFrom',
  'dateTo',
  'createDt',
  'rrDt',
  'fixTariffDateFrom',
  'fixTariffDateTo'
]

/** 日期时间字段列表 */
export const DATETIME_FIELDS = ['orderDt', 'saleDt', 'createTime', 'updateTime']

/** 金额字段列表 */
export const CURRENCY_FIELDS = [
  'dlvPrc',
  'retailPrice',
  'retailAmount',
  'deliveryAmount',
  'returnAmount',
  'deliveryRub',
  'productDiscountForReport',
  'supplierPromo',
  'ppvzSalesCommission',
  'ppvzForPay',
  'ppvzReward',
  'acquiringFee',
  'ppvzVw',
  'ppvzVwNds',
  'penalty',
  'additionalPayment',
  'rebillLogisticCost',
  'storageFee',
  'deduction',
  'acceptance',
  'installmentCofinancingAmount',
  'wibesWbDiscountPercent',
  'cashbackAmount',
  'cashbackDiscount',
  'cashbackCommissionChange',
  'retailPriceWithdiscRub'
]

// ===================== 辅助函数 =====================

/** 驼峰转下划线 */
export const toSnakeCase = (value: string): string =>
  value
    .replace(/([A-Z])/g, '_$1')
    .toLowerCase()
    .replace(/^_/, '')

// ===================== 表格列配置 =====================

/**
 * WB 财务报表明细表格列配置（不含操作列）
 * 操作列由各页面自行添加
 */
export const wbReportDetailColumns: ProColumns[] = [
  {
    title: '店铺ID',
    dataIndex: 'shopId',
    width: 80,
    fixed: 'left'
  },
  {
    title: '周期类型',
    dataIndex: 'periodType',
    width: 80,
    fixed: 'left'
  },
  {
    title: '报表ID',
    dataIndex: 'realizationreportId',
    width: 140
  },
  {
    title: '报表开始日期',
    dataIndex: 'dateFrom',
    width: 120
  },
  {
    title: '报表结束日期',
    dataIndex: 'dateTo',
    width: 120
  },
  {
    title: '记录生成日期',
    dataIndex: 'createDt',
    width: 120
  },
  {
    title: '币种名称',
    dataIndex: 'currencyName',
    width: 100
  },
  {
    title: '合同编码',
    dataIndex: 'suppliercontractCode',
    width: 160
  },
  {
    title: '行ID',
    dataIndex: 'rrdId',
    width: 120
  },
  {
    title: '物流收货ID',
    dataIndex: 'giId',
    width: 140
  },
  {
    title: '配送价格',
    dataIndex: 'dlvPrc',
    width: 120,
    align: 'right'
  },
  {
    title: '固定费率开始日期',
    dataIndex: 'fixTariffDateFrom',
    width: 160
  },
  {
    title: '固定费率结束日期',
    dataIndex: 'fixTariffDateTo',
    width: 160
  },
  {
    title: '品类名称',
    dataIndex: 'subjectName',
    width: 150
  },
  {
    title: '商品ID',
    dataIndex: 'nmId',
    width: 130
  },
  {
    title: '品牌',
    dataIndex: 'brandName',
    width: 150
  },
  {
    title: '品类组',
    dataIndex: 'saName',
    width: 150
  },
  {
    title: '尺寸',
    dataIndex: 'tsName',
    width: 100
  },
  {
    title: '条码',
    dataIndex: 'barcode',
    width: 150
  },
  {
    title: '操作类型',
    dataIndex: 'docTypeName',
    width: 130
  },
  {
    title: '数量',
    dataIndex: 'quantity',
    width: 100,
    align: 'right'
  },
  {
    title: '零售价',
    dataIndex: 'retailPrice',
    width: 120,
    align: 'right'
  },
  {
    title: '零售额',
    dataIndex: 'retailAmount',
    width: 120,
    align: 'right'
  },
  {
    title: '折扣百分比',
    dataIndex: 'salePercent',
    width: 130,
    align: 'right'
  },
  {
    title: '佣金百分比',
    dataIndex: 'commissionPercent',
    width: 130,
    align: 'right'
  },
  {
    title: '仓库名称',
    dataIndex: 'officeName',
    width: 150
  },
  {
    title: '供应商操作名称',
    dataIndex: 'supplierOperName',
    width: 180
  },
  {
    title: '订单时间',
    dataIndex: 'orderDt',
    width: 180
  },
  {
    title: '销售时间',
    dataIndex: 'saleDt',
    width: 180
  },
  {
    title: '报表日期',
    dataIndex: 'rrDt',
    width: 140
  },
  {
    title: '货品ID',
    dataIndex: 'shkId',
    width: 130
  },
  {
    title: '折后价(RUB)',
    dataIndex: 'retailPriceWithdiscRub',
    width: 150,
    align: 'right'
  },
  {
    title: '配送金额',
    dataIndex: 'deliveryAmount',
    width: 130,
    align: 'right'
  },
  {
    title: '退货金额',
    dataIndex: 'returnAmount',
    width: 130,
    align: 'right'
  },
  {
    title: '配送金额(RUB)',
    dataIndex: 'deliveryRub',
    width: 150,
    align: 'right'
  },
  {
    title: '物流箱型',
    dataIndex: 'giBoxTypeName',
    width: 140
  },
  {
    title: '产品折扣',
    dataIndex: 'productDiscountForReport',
    width: 130,
    align: 'right'
  },
  {
    title: '供应商促销金额',
    dataIndex: 'supplierPromo',
    width: 160,
    align: 'right'
  },
  {
    title: 'SPP百分比',
    dataIndex: 'ppvzSppPrc',
    width: 130,
    align: 'right'
  },
  {
    title: '基础KWV百分比',
    dataIndex: 'ppvzKvwPrcBase',
    width: 160,
    align: 'right'
  },
  {
    title: 'KWV百分比',
    dataIndex: 'ppvzKvwPrc',
    width: 130,
    align: 'right'
  },
  {
    title: '评级加成百分比',
    dataIndex: 'supRatingPrcUp',
    width: 160,
    align: 'right'
  },
  {
    title: '是否KGV2模式',
    dataIndex: 'isKgvpV2',
    width: 150
  },
  {
    title: '销售佣金',
    dataIndex: 'ppvzSalesCommission',
    width: 130,
    align: 'right'
  },
  {
    title: '应付金额',
    dataIndex: 'ppvzForPay',
    width: 130,
    align: 'right'
  },
  {
    title: '奖励金额',
    dataIndex: 'ppvzReward',
    width: 130,
    align: 'right'
  },
  {
    title: '收单手续费',
    dataIndex: 'acquiringFee',
    width: 140,
    align: 'right'
  },
  {
    title: '收单费百分比',
    dataIndex: 'acquiringPercent',
    width: 150,
    align: 'right'
  },
  {
    title: '支付处理方式',
    dataIndex: 'paymentProcessing',
    width: 150
  },
  {
    title: '收单银行',
    dataIndex: 'acquiringBank',
    width: 130
  },
  {
    title: 'VW金额',
    dataIndex: 'ppvzVw',
    width: 120,
    align: 'right'
  },
  {
    title: 'VW金额(含税)',
    dataIndex: 'ppvzVwNds',
    width: 150,
    align: 'right'
  },
  {
    title: '仓库名称(结算)',
    dataIndex: 'ppvzOfficeName',
    width: 180
  },
  {
    title: '仓库ID(结算)',
    dataIndex: 'ppvzOfficeId',
    width: 160
  },
  {
    title: '供应商ID',
    dataIndex: 'ppvzSupplierId',
    width: 140
  },
  {
    title: '供应商名称',
    dataIndex: 'ppvzSupplierName',
    width: 160
  },
  {
    title: '供应商税号',
    dataIndex: 'ppvzInn',
    width: 150
  },
  {
    title: '报关单号',
    dataIndex: 'declarationNumber',
    width: 160
  },
  {
    title: '奖励类型',
    dataIndex: 'bonusTypeName',
    width: 140
  },
  {
    title: '贴纸ID',
    dataIndex: 'stickerId',
    width: 140
  },
  {
    title: '站点国家',
    dataIndex: 'siteCountry',
    width: 140
  },
  {
    title: '是否DBS订单',
    dataIndex: 'srvDbs',
    width: 150
  },
  {
    title: '罚款金额',
    dataIndex: 'penalty',
    width: 130,
    align: 'right'
  },
  {
    title: '附加支付',
    dataIndex: 'additionalPayment',
    width: 130,
    align: 'right'
  },
  {
    title: '物流再计费成本',
    dataIndex: 'rebillLogisticCost',
    width: 180,
    align: 'right'
  },
  {
    title: '物流再计费组织',
    dataIndex: 'rebillLogisticOrg',
    width: 180
  },
  {
    title: '仓储费',
    dataIndex: 'storageFee',
    width: 130,
    align: 'right'
  },
  {
    title: '扣款',
    dataIndex: 'deduction',
    width: 130,
    align: 'right'
  },
  {
    title: '验收入库',
    dataIndex: 'acceptance',
    width: 130,
    align: 'right'
  },
  {
    title: '装配单ID',
    dataIndex: 'assemblyId',
    width: 150
  },
  {
    title: 'KIZ码',
    dataIndex: 'kiz',
    width: 130
  },
  {
    title: '唯一订单行ID',
    dataIndex: 'srid',
    width: 200
  },
  {
    title: '报表类型',
    dataIndex: 'reportType',
    width: 130
  },
  {
    title: '是否法人主体',
    dataIndex: 'isLegalEntity',
    width: 150
  },
  {
    title: 'TRBX ID',
    dataIndex: 'trbxId',
    width: 140
  },
  {
    title: '分期共同融资金额',
    dataIndex: 'installmentCofinancingAmount',
    width: 200,
    align: 'right'
  },
  {
    title: 'WB折扣百分比',
    dataIndex: 'wibesWbDiscountPercent',
    width: 160,
    align: 'right'
  },
  {
    title: '返现金额',
    dataIndex: 'cashbackAmount',
    width: 130,
    align: 'right'
  },
  {
    title: '返现折扣',
    dataIndex: 'cashbackDiscount',
    width: 130,
    align: 'right'
  },
  {
    title: '返现佣金变化',
    dataIndex: 'cashbackCommissionChange',
    width: 180,
    align: 'right'
  },
  {
    title: '订单UID',
    dataIndex: 'orderUid',
    width: 220
  },
  {
    title: '支付计划',
    dataIndex: 'paymentSchedule',
    width: 130
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180
  }
]

/**
 * 获取带操作列的完整表格列配置
 */
export const getColumnsWithOperate = (): ProColumns[] => [
  ...wbReportDetailColumns,
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 100,
    fixed: 'right'
  }
]
