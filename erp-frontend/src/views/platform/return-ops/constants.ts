import type { ReturnStatus, ReturnZone, QcResult } from '@/api/wms/return-qc/types'

/** 退货单状态文案 */
export const RETURN_STATUS_TEXT: Record<ReturnStatus, string> = {
  RETURN_PENDING: '待退货收货',
  QC_PENDING: '待质检',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}

/** 状态徽标色 */
export const RETURN_STATUS_BADGE: Record<ReturnStatus, string> = {
  RETURN_PENDING: 'warning',
  QC_PENDING: 'processing',
  COMPLETED: 'success',
  CLOSED: 'default'
}

/** 状态筛选下拉 */
export const RETURN_STATUS_OPTIONS: { label: string; value: ReturnStatus }[] = [
  { label: '待退货收货', value: 'RETURN_PENDING' },
  { label: '待质检', value: 'QC_PENDING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已关闭', value: 'CLOSED' }
]

/** 回库分区文案 */
export const RETURN_ZONE_TEXT: Record<ReturnZone, string> = {
  RETURN: '退货区',
  STANDARD: '标准区',
  DEFECTIVE: '不良品区'
}

/** 质检通过时可选分区（均为 GOOD，可分配） */
export const PASS_ZONE_OPTIONS = [
  { label: '退货区', value: 'RETURN' as ReturnZone },
  { label: '标准区', value: 'STANDARD' as ReturnZone }
]

/** 质检结果文案 */
export const QC_RESULT_TEXT: Record<QcResult, string> = {
  PASS: '质检通过',
  FAIL: '质检失败',
  MIXED: '部分良品'
}

/** 分区 → 品质（PASS 系分区=GOOD，不良品区=DAMAGED） */
export function qualityOfZone(zone: ReturnZone): 'GOOD' | 'DAMAGED' {
  return zone === 'DEFECTIVE' ? 'DAMAGED' : 'GOOD'
}
