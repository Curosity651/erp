import { describe, expect, it } from 'vitest'
import ExcelJS from 'exceljs'
import {
  buildReturnReceiptTemplateBuffer,
  parseReturnReceiptTemplateBuffer,
  validateReturnReceiptRows
} from './return-receipt-excel'

describe('return receipt Excel import', () => {
  it('builds the exact input columns and an instruction sheet', async () => {
    const buffer = await buildReturnReceiptTemplateBuffer()
    const workbook = new ExcelJS.Workbook()
    await workbook.xlsx.load(buffer)

    expect(workbook.worksheets.map(sheet => sheet.name)).toEqual(['退货商品导入', '填写说明'])
    const input = workbook.getWorksheet('退货商品导入')!
    expect(input.getRow(1).values).toEqual([
      undefined,
      '全局 SKU',
      '实收数量',
      '平台订单号',
      '退货原因'
    ])
    expect(await parseReturnReceiptTemplateBuffer(buffer)).toEqual([])
  })

  it('round-trips valid rows and normalizes text', async () => {
    const workbook = new ExcelJS.Workbook()
    const sheet = workbook.addWorksheet('退货商品导入')
    sheet.addRow(['全局 SKU', '实收数量', '平台订单号', '退货原因'])
    sheet.addRow([' jhin-sku-1 ', 2, ' OZON-1 ', 'damaged'])

    const rows = await parseReturnReceiptTemplateBuffer(await workbook.xlsx.writeBuffer())
    expect(rows).toEqual([
      {
        excelRow: 2,
        warehouseSkuCode: 'JHIN-SKU-1',
        receivedQty: 2,
        platformOrderId: 'OZON-1',
        returnReason: 'DAMAGED'
      }
    ])
  })

  it.each([
    {
      name: 'duplicate normalized SKU',
      rows: [
        { excelRow: 2, warehouseSkuCode: 'JHIN-SKU-1', receivedQty: 1 },
        { excelRow: 3, warehouseSkuCode: ' jhin-sku-1 ', receivedQty: 2 }
      ],
      error: '重复'
    },
    {
      name: 'non-positive quantity',
      rows: [{ excelRow: 2, warehouseSkuCode: 'JHIN-SKU-1', receivedQty: 0 }],
      error: '正整数'
    },
    {
      name: 'fractional quantity',
      rows: [{ excelRow: 2, warehouseSkuCode: 'JHIN-SKU-1', receivedQty: 1.5 }],
      error: '正整数'
    },
    {
      name: 'unsupported reason',
      rows: [
        {
          excelRow: 2,
          warehouseSkuCode: 'JHIN-SKU-1',
          receivedQty: 1,
          returnReason: 'INVALID'
        }
      ],
      error: '退货原因'
    }
  ])('rejects $name', ({ rows, error }) => {
    const result = validateReturnReceiptRows(rows)
    expect(result.lines).toEqual([])
    expect(result.errors.join('\n')).toContain(error)
  })

  it('rejects a changed header', async () => {
    const workbook = new ExcelJS.Workbook()
    const sheet = workbook.addWorksheet('退货商品导入')
    sheet.addRow(['SKU', '实收数量', '平台订单号', '退货原因'])
    await expect(
      parseReturnReceiptTemplateBuffer(await workbook.xlsx.writeBuffer())
    ).rejects.toThrow('表头')
  })

  it('rejects more than 500 non-empty rows', () => {
    const rows = Array.from({ length: 501 }, (_, index) => ({
      excelRow: index + 2,
      warehouseSkuCode: `JHIN-SKU-${index}`,
      receivedQty: 1
    }))
    expect(validateReturnReceiptRows(rows).errors.join('\n')).toContain('500')
  })
})
