import type { ReturnStatus, ReturnZone, QcResult } from '@/api/wms/return-qc/types'

/** Translation keys for return statuses. */
export const RETURN_STATUS_I18N_KEYS: Record<ReturnStatus, string> = {
  PENDING_OWNER: 'platform.return.status.pendingOwner',
  PENDING_OPERATION: 'platform.return.status.pendingOperation',
  COMPLETED: 'platform.return.status.completed',
  CLOSED: 'platform.return.status.closed'
}

/** 状态徽标色 */
export const RETURN_STATUS_BADGE: Record<ReturnStatus, string> = {
  PENDING_OWNER: 'warning',
  PENDING_OPERATION: 'processing',
  COMPLETED: 'success',
  CLOSED: 'default'
}

/** Translation keys for stock zones. */
export const RETURN_ZONE_I18N_KEYS: Record<ReturnZone, string> = {
  RETURN: 'platform.return.process.returnZone',
  STANDARD: 'platform.return.process.standardZone',
  DEFECTIVE: 'platform.return.process.defectiveZone'
}

/** Available zones for items that pass QC. */
export const PASS_ZONE_VALUES: ReturnZone[] = ['RETURN', 'STANDARD']

/** Translation keys for QC results. */
export const QC_RESULT_I18N_KEYS: Record<QcResult, string> = {
  PASS: 'platform.return.qc.pass',
  FAIL: 'platform.return.qc.fail',
  MIXED: 'platform.return.qc.mixed'
}

/** 分区 → 品质（PASS 系分区=GOOD，不良品区=DAMAGED） */
export function qualityOfZone(zone: ReturnZone): 'GOOD' | 'DAMAGED' {
  return zone === 'DEFECTIVE' ? 'DAMAGED' : 'GOOD'
}
