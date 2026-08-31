import type { LogisticsProductVO } from '@/api/wms/logistics-product/types'

export function isPricingLocked(product?: Pick<LogisticsProductVO, 'used' | 'orderReferenceCount'>) {
  return product?.used === true || Number(product?.orderReferenceCount || 0) > 0
}

export function suggestVersionCode(productCode: string) {
  const normalized = (productCode || '').trim().toUpperCase()
  const match = normalized.match(/^(.*)-V(\d+)$/)
  if (!match) return `${normalized}-V2`
  return `${match[1]}-V${Number(match[2]) + 1}`
}
