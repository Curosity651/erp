export interface ManualInboundTemplateRow {
  inboundNo: string
  inboundDate: string
  warehouseId: string
  skuCode: string
  expectedQuantity: number
  remark: string
}

export interface ManualInboundTemplateDefaults {
  inboundNo?: string
  inboundDate?: string
  warehouseId?: number
}

export const MANUAL_INBOUND_TEMPLATE_HEADERS = [
  '入库单号',
  '入库日期',
  '入库仓库ID',
  'SKU编码',
  '应到数量',
  '备注'
] as const

const TEMPLATE_SHEET = '商品入库'
const GUIDE_SHEET = '填写说明'

export async function buildManualInboundTemplateBuffer(
  defaults: ManualInboundTemplateDefaults = {}
) {
  const ExcelJS = await loadExcelJs()
  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'HYLDSys ERP'
  workbook.created = new Date()

  const sheet = workbook.addWorksheet(TEMPLATE_SHEET, { views: [{ state: 'frozen', ySplit: 1 }] })
  sheet.columns = [
    { header: MANUAL_INBOUND_TEMPLATE_HEADERS[0], key: 'inboundNo', width: 24 },
    { header: MANUAL_INBOUND_TEMPLATE_HEADERS[1], key: 'inboundDate', width: 16 },
    { header: MANUAL_INBOUND_TEMPLATE_HEADERS[2], key: 'warehouseId', width: 16 },
    { header: MANUAL_INBOUND_TEMPLATE_HEADERS[3], key: 'skuCode', width: 24 },
    { header: MANUAL_INBOUND_TEMPLATE_HEADERS[4], key: 'expectedQuantity', width: 14 },
    { header: MANUAL_INBOUND_TEMPLATE_HEADERS[5], key: 'remark', width: 32 }
  ]
  sheet.addRow({
    inboundNo: defaults.inboundNo || '',
    inboundDate: defaults.inboundDate || '',
    warehouseId: defaults.warehouseId ?? '',
    skuCode: '',
    expectedQuantity: '',
    remark: ''
  })
  styleHeader(sheet.getRow(1))
  sheet.autoFilter = 'A1:F2'
  sheet.getRow(2).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFFFF7E6' }
  }

  const guide = workbook.addWorksheet(GUIDE_SHEET)
  guide.columns = [
    { header: '项目', key: 'name', width: 20 },
    { header: '说明', key: 'description', width: 80 }
  ]
  guide.addRows([
    { name: '填写范围', description: '请在“商品入库”工作表中填写，每一行代表一个 SKU。' },
    { name: '入库单号', description: '同一张入库单的每一行必须保持一致，必填。' },
    { name: '入库日期', description: '格式：YYYY-MM-DD，同一张入库单的每一行必须保持一致，必填。' },
    { name: '入库仓库ID', description: '填写系统中自有仓库（OWN）的 ID，同一张入库单的每一行必须保持一致，必填。' },
    { name: 'SKU编码', description: '必须是系统 SKU 资料中已存在且已维护外箱长、宽、高和单箱毛重的 SKU。' },
    { name: '应到数量', description: '填写大于 0 的整数。重复 SKU 请先合并数量。' },
    { name: '包装尺寸', description: '导入后自动从 SKU 资料读取外箱长、宽、高，并按数量统计总长、总宽、总高和总体积。' }
  ])
  styleHeader(guide.getRow(1))

  return workbook.xlsx.writeBuffer()
}

export async function parseManualInboundTemplateFile(file: File): Promise<ManualInboundTemplateRow[]> {
  const ExcelJS = await loadExcelJs()
  const workbook = new ExcelJS.Workbook()
  await workbook.xlsx.load(await file.arrayBuffer() as any)
  const sheet = workbook.getWorksheet(TEMPLATE_SHEET)
  if (!sheet) throw new Error(`模板缺少“${TEMPLATE_SHEET}”工作表`)

  MANUAL_INBOUND_TEMPLATE_HEADERS.forEach((header, index) => {
    if (cellText(sheet.getCell(1, index + 1).value) !== header) {
      throw new Error(`模板第 ${index + 1} 列应为“${header}”，请重新下载模板`)
    }
  })

  const rows: ManualInboundTemplateRow[] = []
  for (let rowNumber = 2; rowNumber <= sheet.rowCount; rowNumber += 1) {
    const row = sheet.getRow(rowNumber)
    const values = Array.from(
      { length: MANUAL_INBOUND_TEMPLATE_HEADERS.length },
      (_, index) => cellText(row.getCell(index + 1).value)
    )
    if (values.every(value => !value)) continue
    rows.push({
      inboundNo: values[0],
      inboundDate: values[1],
      warehouseId: values[2],
      skuCode: values[3],
      expectedQuantity: numericCell(values[4]),
      remark: values[5]
    })
  }
  return rows
}

export async function downloadManualInboundTemplate(
  defaults: ManualInboundTemplateDefaults = {}
) {
  const buffer = await buildManualInboundTemplateBuffer(defaults)
  const blob = new Blob([buffer as BlobPart], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = '商品入库模板.xlsx'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
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
  if (value instanceof Date) {
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return year + '-' + month + '-' + day
  }
  if (typeof value === 'object') {
    if ('result' in value) return cellText(value.result)
    if ('richText' in value) {
      return value.richText.map((part: { text?: string }) => part.text || '').join('')
    }
    if ('text' in value) return String(value.text || '').trim()
  }
  return String(value).trim()
}

function numericCell(value: string): number {
  const number = Number(value)
  return Number.isFinite(number) ? number : Number.NaN
}
