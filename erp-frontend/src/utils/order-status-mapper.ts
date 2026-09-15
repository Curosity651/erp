import { ERP_STATUS_MAP, OWNER_ORDER_STATUS_MAP } from '@/api/order/types'
import type { ErpStatusKey, OwnerOrderBusinessStatus } from '@/api/order/types'

/**
 * 状态映射条目
 */
export interface StatusEntry {
  label: string
  tip: string
  cls: string
}

/**
 * ERP 状态映射（三平台共用）
 * dotClass 格式：status-{状态码小写去特殊字符}，如 status-readytoship
 */
export function mapErpStatus(status?: string): { label: string; dotClass: string; tip: string } {
  const upper = (status || '').toUpperCase() as ErpStatusKey
  const mapping = ERP_STATUS_MAP[upper]
  const dotClass = status
    ? 'status-' + status.toLowerCase().replace(/[^a-z]/g, '')
    : 'status-unknown'

  if (mapping) {
    return { label: mapping.label, dotClass, tip: mapping.tip }
  }
  return { label: status || '-', dotClass, tip: status || '未知状态' }
}

export function mapOwnerOrderStatus(
  status?: string
): { label: string; dotClass: string; tip: string } {
  const mapping = OWNER_ORDER_STATUS_MAP[status as OwnerOrderBusinessStatus]
  if (mapping) return mapping
  return { label: status || '-', dotClass: 'status-unknown', tip: status || '未知状态' }
}

/**
 * 工厂函数：根据状态 MAP 创建映射函数
 * MAP 的 key 统一按小写匹配（适用于 WB、Ozon）
 */
export function createStatusMapper(map: Record<string, StatusEntry>) {
  return (code?: string): StatusEntry => {
    const k = (code || '').toLowerCase()
    return map[k] || { label: code || '-', tip: code || '-', cls: '' }
  }
}

/**
 * 工厂函数：根据状态 MAP 创建映射函数
 * MAP 的 key 统一按大写匹配（适用于 Yandex 等大写 key 的 MAP）
 */
export function createUpperStatusMapper(map: Record<string, StatusEntry>) {
  return (code?: string): StatusEntry => {
    const k = (code || '').toUpperCase()
    return map[k] || { label: code || '-', tip: code || '-', cls: '' }
  }
}
