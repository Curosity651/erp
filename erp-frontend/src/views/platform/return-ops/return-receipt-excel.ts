import type { ReturnReceiptDraftLine } from './return-receipt-flow'

export interface ReturnReceiptExcelRow {
  excelRow: number
  warehouseSkuCode: string
  receivedQty: number
  platformOrderId?: string
  returnReason?: string
}

export interface ReturnReceiptExcelValidation {
  lines: ReturnReceiptDraftLine[]
  errors: string[]
}

const INPUT_SHEET = '退货商品导入'
const HELP_SHEET = '填写说明'
const HEADERS = ['全局 SKU', '实收数量', '平台订单号', '退货原因'] as const
const REASONS = ['NOT_WANTED', 'DAMAGED', 'WRONG_ITEM', 'QUALITY_ISSUE', 'OTHER'] as const
const MAX_ROWS = 500

export async function buildReturnReceiptTemplateBuffer() {
  const ExcelJS = await loadExcelJs()
  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'HYLDSys ERP'
  workbook.created = new Date()

  const sheet = workbook.addWorksheet(INPUT_SHEET, {
    views: [{ state: 'frozen', ySplit: 1, showGridLines: false }]
  })
  sheet.columns = [
    { header: HEADERS[0], key: 'warehouseSkuCode', width: 30, style: { numFmt: '@' } },
    { header: HEADERS[1], key: 'receivedQty', width: 14, style: { numFmt: '0' } },
    { header: HEADERS[2], key: 'platformOrderId', width: 26, style: { numFmt: '@' } },
    { header: HEADERS[3], key: 'returnReason', width: 22 }
  ]
  styleHeader(sheet.getRow(1))
  sheet.autoFilter = `A1:D${MAX_ROWS + 1}`
  for (let rowNumber = 2; rowNumber <= MAX_ROWS + 1; rowNumber += 1) {
    sheet.getCell(rowNumber, 1).numFmt = '@'
    sheet.getCell(rowNumber, 3).numFmt = '@'
    sheet.getCell(rowNumber, 4).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: [`"${REASONS.join(',')}"`]
    }
    for (let column = 1; column <= 4; column += 1) {
      sheet.getCell(rowNumber, column).fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: column <= 2 ? 'FFFFF7E6' : 'FFF8FAFC' }
      }
    }
  }

  const help = workbook.addWorksheet(HELP_SHEET, { views: [{ showGridLines: false }] })
  help.columns = [{ width: 22 }, { width: 72 }]
  help.addRows([
    ['字段', '填写规则'],
    ['全局 SKU', '必填。填写或扫描完整仓库 SKU，例如 JHIN-ADNZ-007-QXM-02。'],
    ['实收数量', '必填。只能填写大于 0 的整数。'],
    ['平台订单号', '选填。按文本保存，不会转换长数字。'],
    ['退货原因', `选填。可选值：${REASONS.join('、')}；留空按 OTHER 处理。`],
    ['示例', 'JHIN-ADNZ-007-QXM-02 | 2 | OZON-ORDER-001 | DAMAGED'],
    ['导入限制', '最多 500 条非空商品行。导入只替换页面草稿，不会自动提交退货单。']
  ])
  styleHeader(help.getRow(1))
  help.getColumn(2).alignment = { vertical: 'middle', wrapText: true }

  return workbook.xlsx.writeBuffer()
}

export async function parseReturnReceiptTemplateBuffer(
  buffer: ArrayBuffer | Uint8Array
): Promise<ReturnReceiptExcelRow[]> {
  const ExcelJS = await loadExcelJs()
  const workbook = new ExcelJS.Workbook()
  await workbook.xlsx.load(buffer as any)
  const sheet = workbook.getWorksheet(INPUT_SHEET)
  if (!sheet) throw new Error(`模板缺少“${INPUT_SHEET}”工作表`)
  HEADERS.forEach((header, index) => {
    if (cellText(sheet.getCell(1, index + 1).value) !== header) {
      throw new Error(`模板第 ${index + 1} 列表头应为“${header}”，请重新下载模板`)
    }
  })

  const rows: ReturnReceiptExcelRow[] = []
  for (let rowNumber = 2; rowNumber <= sheet.rowCount; rowNumber += 1) {
    const values = HEADERS.map((_, index) => cellText(sheet.getCell(rowNumber, index + 1).value))
    if (values.every(value => !value)) continue
    rows.push({
      excelRow: rowNumber,
      warehouseSkuCode: values[0].toUpperCase(),
      receivedQty: numericCell(values[1]),
      platformOrderId: values[2] || undefined,
      returnReason: values[3].toUpperCase() || undefined
    })
  }
  return rows
}

export function validateReturnReceiptRows(
  rows: ReturnReceiptExcelRow[]
): ReturnReceiptExcelValidation {
  const errors: string[] = []
  if (rows.length > MAX_ROWS) errors.push(`导入数据不能超过 ${MAX_ROWS} 行`)
  const firstRowBySku = new Map<string, number>()
  const lines: ReturnReceiptDraftLine[] = []

  rows.forEach(row => {
    const sku = String(row.warehouseSkuCode || '')
      .trim()
      .toUpperCase()
    if (!sku) errors.push(`第 ${row.excelRow} 行：全局 SKU 不能为空`)
    const firstRow = firstRowBySku.get(sku)
    if (sku && firstRow !== undefined) {
      errors.push(`第 ${row.excelRow} 行：全局 SKU 与第 ${firstRow} 行重复`)
    } else if (sku) {
      firstRowBySku.set(sku, row.excelRow)
    }
    const quantity = Number(row.receivedQty)
    if (!Number.isInteger(quantity) || quantity <= 0) {
      errors.push(`第 ${row.excelRow} 行：实收数量必须是大于 0 的正整数`)
    }
    const reason = String(row.returnReason || 'OTHER')
      .trim()
      .toUpperCase()
    if (!REASONS.includes(reason as (typeof REASONS)[number])) {
      errors.push(`第 ${row.excelRow} 行：退货原因不在允许范围内`)
    }
    lines.push({
      key: row.excelRow,
      warehouseSkuCode: sku,
      receivedQty: quantity,
      platformOrderId: String(row.platformOrderId || '').trim() || undefined,
      returnReason: reason,
      photoFileIds: [],
      uploading: false
    })
  })

  return errors.length ? { lines: [], errors: limitedErrors(errors) } : { lines, errors: [] }
}

export async function parseReturnReceiptTemplateFile(file: File) {
  if (!file.name.toLowerCase().endsWith('.xlsx')) throw new Error('只支持 .xlsx 文件')
  return parseReturnReceiptTemplateBuffer(await file.arrayBuffer())
}

export async function downloadReturnReceiptTemplate() {
  const buffer = await buildReturnReceiptTemplateBuffer()
  const blob = new Blob([buffer as BlobPart], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = '退货商品导入模板.xlsx'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

function limitedErrors(errors: string[]) {
  if (errors.length <= 20) return errors
  return [...errors.slice(0, 20), `另有 ${errors.length - 20} 条错误未显示`]
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
    if ('richText' in value) {
      return value.richText
        .map((part: { text?: string }) => part.text || '')
        .join('')
        .trim()
    }
    if ('text' in value) return String(value.text || '').trim()
  }
  return String(value).trim()
}

function numericCell(value: string) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : Number.NaN
}
