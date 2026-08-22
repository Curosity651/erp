import type { PutawayRecordContextVO } from '@/api/wms/inbound-execution'
import type { PutawayRecordFormLine } from './putaway-record'
import { projectLocationCapacity, requiresCapacityReason } from './putaway-record'

export interface PutawayTemplateRow {
  inboundNo: string
  warehouseSkuCode: string
  skuName: string
  receivedQuantity: number
  quality: string
  locationCode: string
  quantity?: number
  capacityOverrideReason: string
}

export interface PutawayTemplateValidationResult {
  lines: PutawayRecordFormLine[]
  errors: string[]
}

const TEMPLATE_SHEET = '上架记录'
const LOCATION_SHEET = '可用库位'
const TEMPLATE_HEADERS = ['入库单号', '内部SKU', '商品名称', '实收数量', '品质', '实际库位', '本次数量', '超限说明'] as const

export function createTemplateRows(context: PutawayRecordContextVO): PutawayTemplateRow[] {
  return context.items.map(item => ({
    inboundNo: context.inboundNo,
    warehouseSkuCode: item.warehouseSkuCode || item.skuCode,
    skuName: item.skuName || '',
    receivedQuantity: item.receivedQuantity,
    quality: '良品',
    locationCode: '',
    quantity: undefined,
    capacityOverrideReason: ''
  }))
}

export function validateTemplateRows(
  sourceRows: PutawayTemplateRow[],
  context: PutawayRecordContextVO
): PutawayTemplateValidationResult {
  const errors: string[] = []
  const itemsByWarehouseSku = new Map(context.items.map(item => [
    String(item.warehouseSkuCode || item.skuCode).trim().toUpperCase(),
    item
  ]))
  const locationsByCode = new Map(context.locations.map(location => [
    String(location.locationCode).trim().toUpperCase(),
    location
  ]))
  const parsed: PutawayRecordFormLine[] = []
  const importedQuantityBySku = new Map<string, number>()

  sourceRows.forEach((row, index) => {
    const excelRow = index + 2
    const inboundNo = String(row.inboundNo || '').trim()
    const warehouseSkuCode = String(row.warehouseSkuCode || '').trim().toUpperCase()
    const locationCode = String(row.locationCode || '').trim().toUpperCase()
    const isEmpty = !inboundNo && !warehouseSkuCode && !locationCode && !row.quantity
    if (isEmpty) return

    if (inboundNo !== context.inboundNo) errors.push(`第 ${excelRow} 行：入库单号与当前入库单不一致`)
    const item = itemsByWarehouseSku.get(warehouseSkuCode)
    if (!item) errors.push(`第 ${excelRow} 行：内部 SKU 不属于当前入库单`)
    else if (Number(row.receivedQuantity) !== item.receivedQuantity) {
      errors.push(`第 ${excelRow} 行：实收数量与当前入库单不一致`)
    }

    const location = locationsByCode.get(locationCode)
    if (!location) errors.push(`第 ${excelRow} 行：库位编码不存在或当前货主无权使用`)
    const quality = qualityCode(row.quality)
    if (!quality) errors.push(`第 ${excelRow} 行：品质只能填写良品或不良品`)
    if (location && quality === 'DAMAGED' && location.zoneType !== 'DEFECTIVE') {
      errors.push(`第 ${excelRow} 行：不良品只能放入不良品区`)
    }
    if (location && quality === 'GOOD' && location.zoneType === 'DEFECTIVE') {
      errors.push(`第 ${excelRow} 行：良品不能放入不良品区`)
    }
    const quantity = Number(row.quantity)
    if (!Number.isInteger(quantity) || quantity <= 0) errors.push(`第 ${excelRow} 行：本次数量必须是大于 0 的整数`)
	if (item && Number.isInteger(quantity) && quantity > 0) {
	  importedQuantityBySku.set(item.skuCode, (importedQuantityBySku.get(item.skuCode) || 0) + quantity)
	}

    if (item && location && quality && Number.isInteger(quantity) && quantity > 0) {
      parsed.push({
        key: `excel-row-${excelRow}`,
        skuCode: item.skuCode,
        quality,
        locationId: location.locationId,
        quantity,
        capacityOverrideReason: String(row.capacityOverrideReason || '').trim() || undefined
      })
    }
  })

  context.items.forEach(item => {
    const allocated = importedQuantityBySku.get(item.skuCode) || 0
    if (allocated !== item.receivedQuantity) {
      errors.push(`${item.warehouseSkuCode || item.skuCode}：模板合计 ${allocated} 件，实收 ${item.receivedQuantity} 件`)
    }
  })

  const skuByCode = new Map(context.items.map(item => [item.skuCode, item]))
  const locationById = new Map(context.locations.map(location => [location.locationId, location]))
  parsed.forEach((line, index) => {
    const location = line.locationId ? locationById.get(line.locationId) : undefined
    if (!location) return
    const projection = projectLocationCapacity(location, parsed, skuByCode)
    if (requiresCapacityReason(projection) && !line.capacityOverrideReason?.trim()) {
      errors.push(`第 ${index + 2} 行：体积或承重超限，必须填写超限说明`)
    }
  })

  return errors.length ? { lines: [], errors: unique(errors) } : { lines: parsed, errors: [] }
}

export async function buildPutawayTemplateBuffer(context: PutawayRecordContextVO) {
  const ExcelJS = await loadExcelJs()
  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'HYLDSys ERP'
  workbook.created = new Date()

  const sheet = workbook.addWorksheet(TEMPLATE_SHEET, { views: [{ state: 'frozen', ySplit: 1 }] })
  sheet.columns = [
    { header: TEMPLATE_HEADERS[0], key: 'inboundNo', width: 24 },
    { header: TEMPLATE_HEADERS[1], key: 'warehouseSkuCode', width: 24 },
    { header: TEMPLATE_HEADERS[2], key: 'skuName', width: 28 },
    { header: TEMPLATE_HEADERS[3], key: 'receivedQuantity', width: 12 },
    { header: TEMPLATE_HEADERS[4], key: 'quality', width: 12 },
    { header: TEMPLATE_HEADERS[5], key: 'locationCode', width: 18 },
    { header: TEMPLATE_HEADERS[6], key: 'quantity', width: 12 },
    { header: TEMPLATE_HEADERS[7], key: 'capacityOverrideReason', width: 32 }
  ]
  createTemplateRows(context).forEach(row => sheet.addRow(row))
  styleHeader(sheet.getRow(1))
  sheet.autoFilter = `A1:H${Math.max(sheet.rowCount, 1)}`

  const locationSheet = workbook.addWorksheet(LOCATION_SHEET)
  locationSheet.columns = [
    { header: '库位编码', key: 'locationCode', width: 20 },
    { header: '分区名称', key: 'zoneName', width: 20 },
    { header: '分区类型', key: 'zoneType', width: 16 },
    { header: '当前容量使用率', key: 'utilizationPercent', width: 18 },
    { header: '公共库位', key: 'publicShared', width: 12 }
  ]
  context.locations.forEach(location => locationSheet.addRow({
    locationCode: location.locationCode,
    zoneName: location.zoneName || '',
    zoneType: location.zoneType || '',
    utilizationPercent: location.capacityCalculable ? `${Number(location.utilizationPercent || 0).toFixed(1)}%` : '容量未知',
    publicShared: location.publicShared === 1 ? '是' : '否'
  }))
  styleHeader(locationSheet.getRow(1))
  locationSheet.autoFilter = `A1:E${Math.max(locationSheet.rowCount, 1)}`

  const validationEndRow = sheet.rowCount + 100
  for (let rowNumber = 2; rowNumber <= validationEndRow; rowNumber += 1) {
    sheet.getCell(`E${rowNumber}`).dataValidation = {
      type: 'list',
      allowBlank: false,
      formulae: ['"良品,不良品"']
    }
    if (context.locations.length) {
      sheet.getCell(`F${rowNumber}`).dataValidation = {
        type: 'list',
        allowBlank: false,
        formulae: [`'${LOCATION_SHEET}'!$A$2:$A$${context.locations.length + 1}`]
      }
    }
    for (let column = 5; column <= 8; column += 1) {
      sheet.getCell(rowNumber, column).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFF7E6' } }
    }
  }

  return workbook.xlsx.writeBuffer()
}

export async function parsePutawayTemplateBuffer(buffer: ArrayBuffer | Uint8Array): Promise<PutawayTemplateRow[]> {
  const ExcelJS = await loadExcelJs()
  const workbook = new ExcelJS.Workbook()
  await workbook.xlsx.load(buffer as any)
  const sheet = workbook.getWorksheet(TEMPLATE_SHEET)
  if (!sheet) throw new Error(`模板缺少“${TEMPLATE_SHEET}”工作表`)

  TEMPLATE_HEADERS.forEach((header, index) => {
    if (cellText(sheet.getCell(1, index + 1).value) !== header) {
      throw new Error(`模板第 ${index + 1} 列应为“${header}”，请重新下载模板`)
    }
  })

  const rows: PutawayTemplateRow[] = []
  for (let rowNumber = 2; rowNumber <= sheet.rowCount; rowNumber += 1) {
    const row = sheet.getRow(rowNumber)
    const values = Array.from({ length: TEMPLATE_HEADERS.length }, (_, index) => cellText(row.getCell(index + 1).value))
    if (values.every(value => !value)) continue
    rows.push({
      inboundNo: values[0],
      warehouseSkuCode: values[1],
      skuName: values[2],
      receivedQuantity: numericCell(values[3]),
      quality: values[4],
      locationCode: values[5],
      quantity: values[6] ? numericCell(values[6]) : undefined,
      capacityOverrideReason: values[7]
    })
  }
  return rows
}

export async function downloadPutawayTemplate(context: PutawayRecordContextVO) {
  const buffer = await buildPutawayTemplateBuffer(context)
  const blob = new Blob([buffer as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `上架记录模板-${context.inboundNo}.xlsx`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export async function parsePutawayTemplateFile(file: File) {
  return parsePutawayTemplateBuffer(await file.arrayBuffer())
}

function qualityCode(value: unknown): 'GOOD' | 'DAMAGED' | undefined {
  const text = String(value || '').trim().toUpperCase()
  if (text === '良品' || text === 'GOOD') return 'GOOD'
  if (text === '不良品' || text === 'DAMAGED') return 'DAMAGED'
  return undefined
}

function unique(values: string[]) {
  return [...new Set(values)]
}

async function loadExcelJs() {
  const module = await import('exceljs')
  return (module.default || module) as typeof import('exceljs')
}

function styleHeader(row: any) {
  row.font = { bold: true, color: { argb: 'FFFFFFFF' } }
  row.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1677FF' } }
  row.alignment = { vertical: 'middle', horizontal: 'center' }
  row.height = 24
}

function cellText(value: any): string {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object') {
    if ('result' in value) return cellText(value.result)
    if ('richText' in value) return value.richText.map((part: { text?: string }) => part.text || '').join('')
    if ('text' in value) return String(value.text || '').trim()
  }
  return String(value).trim()
}

function numericCell(value: string): number {
  const number = Number(value)
  return Number.isFinite(number) ? number : Number.NaN
}
