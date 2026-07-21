import type {
  ReconciliationStatus,
  PeriodType,
  FinancialRecordType
} from '@/api/financial/reconciliation/types'

// 复用状态映射工具
export { mapErpStatus, mapWbPlatformStatus, mapWbSupplierStatus } from '@/utils/wb-status-mapper'

// ===================== 周期类型相关 =====================

/**
 * 周期类型选项
 */
export const PERIOD_TYPE_OPTIONS: { label: string; value: PeriodType }[] = [
  { label: '周报', value: 'weekly' },
  { label: '日报', value: 'daily' }
]

// ===================== 对账状态相关 =====================

/**
 * 对账状态选项（用于多选筛选）
 */
export const RECONCILIATION_STATUS_OPTIONS: { label: string; value: ReconciliationStatus }[] = [
  { label: '待履约', value: 'PENDING' },
  { label: '运输中', value: 'IN_TRANSIT' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已对账', value: 'MATCHED' },
  { label: '异常', value: 'ANOMALY' }
]

/**
 * 对账状态标签颜色
 */
export const RECONCILIATION_STATUS_TAG_COLOR: Record<ReconciliationStatus, string> = {
  PENDING: 'default',
  IN_TRANSIT: 'processing',
  CANCELED: 'warning',
  MATCHED: 'success',
  ANOMALY: 'error'
}

/**
 * 对账状态文本映射
 */
export const RECONCILIATION_STATUS_TEXT: Record<ReconciliationStatus, string> = {
  PENDING: '待履约',
  IN_TRANSIT: '运输中',
  CANCELED: '已取消',
  MATCHED: '已对账',
  ANOMALY: '异常'
}

/**
 * 对账状态描述（用于详情页）
 */
export const RECONCILIATION_STATUS_DESC: Record<ReconciliationStatus, string> = {
  PENDING: '订单尚未发货，不需要财务记录',
  IN_TRANSIT: '订单已发货但未交付，尚无财务记录是正常的',
  CANCELED: '订单发货前取消，无需财务记录',
  MATCHED: '订单履约完成，财务记录正常匹配',
  ANOMALY: '订单应有财务记录但缺失或无销售记录，需排查'
}

/**
 * 财务记录类型标签颜色
 */
export const RECORD_TYPE_TAG_COLOR: Record<FinancialRecordType, string> = {
  SALE: 'green',
  RETURN: 'red',
  LOGISTICS: 'blue',
  STORAGE: 'purple',
  PENALTY: 'orange',
  COMPENSATION: 'cyan',
  OTHER: 'default'
}

/**
 * 财务记录类型文本映射
 */
export const RECORD_TYPE_TEXT: Record<FinancialRecordType, string> = {
  SALE: '销售',
  RETURN: '退货',
  LOGISTICS: '物流',
  STORAGE: '仓储',
  PENALTY: '罚款',
  COMPENSATION: '补偿',
  OTHER: '其他'
}
