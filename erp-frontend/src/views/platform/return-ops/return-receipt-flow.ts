import type { ReturnReceiptDTO, WarehouseSkuResolveResult } from '@/api/wms/return-qc/types'

export interface ReturnReceiptDraftLine {
  key: number
  warehouseSkuCode: string
  receivedQty: number
  platformOrderId?: string
  returnReason: string
  photoFileIds: number[]
  uploading: boolean
  resolving?: boolean
  matched?: boolean
  ownerName?: string
  originalSkuCode?: string
  skuName?: string
  resolveError?: string
}

export interface ReturnReceiptDraftForm {
  warehouseId?: number
  returnDate?: string
  remark?: string
}

function normalizedSku(value: string | undefined) {
  return (value || '').trim().toUpperCase()
}

function cloneLines(lines: ReturnReceiptDraftLine[]) {
  return lines.map(line => ({ ...line, photoFileIds: [...line.photoFileIds] }))
}

export function applyResolvedReturnSkus(
  lines: ReturnReceiptDraftLine[],
  results: WarehouseSkuResolveResult[]
): { lines: ReturnReceiptDraftLine[]; errors: string[] } {
  const byRequestedCode = new Map(
    results.map(result => [normalizedSku(result.requestedCode), result] as const)
  )
  const errors: string[] = []

  lines.forEach((line, index) => {
    const result = byRequestedCode.get(normalizedSku(line.warehouseSkuCode))
    if (!result || !result.matched) {
      errors.push(`第 ${index + 1} 行：${result?.error || '全局 SKU 未返回解析结果'}`)
    }
  })
  if (errors.length) return { lines: cloneLines(lines), errors }

  return {
    errors: [],
    lines: lines.map(line => {
      const result = byRequestedCode.get(normalizedSku(line.warehouseSkuCode))!
      return {
        ...line,
        photoFileIds: [...line.photoFileIds],
        warehouseSkuCode: result.warehouseSkuCode,
        ownerName: result.ownerName,
        originalSkuCode: result.skuCode,
        skuName: result.skuName,
        matched: true,
        resolving: false,
        resolveError: undefined
      }
    })
  }
}

export function buildReturnReceiptPayload(
  form: ReturnReceiptDraftForm,
  lines: ReturnReceiptDraftLine[]
): ReturnReceiptDTO {
  return {
    warehouseId: form.warehouseId!,
    returnDate: form.returnDate,
    remark: form.remark?.trim() || undefined,
    items: lines.map(line => ({
      warehouseSkuCode: line.warehouseSkuCode.trim(),
      receivedQty: line.receivedQty,
      platformOrderId: line.platformOrderId?.trim() || undefined,
      returnReason: line.returnReason || 'OTHER',
      photoFileIds: [...line.photoFileIds]
    }))
  }
}
