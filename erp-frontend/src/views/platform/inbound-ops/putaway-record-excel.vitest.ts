import { describe, expect, it } from 'vitest'
import type { PutawayRecordContextVO } from '@/api/wms/inbound-execution'
import {
  buildPutawayTemplateBuffer,
  createTemplateRows,
  parsePutawayTemplateBuffer,
  validateTemplateRows
} from './putaway-record-excel'

const context: PutawayRecordContextVO = {
  inboundOrderId: 1,
  inboundNo: 'JHIN-RK-001',
  warehouseId: 8,
  erpTenantId: 10,
  ownerName: 'JHIN',
  items: [{
    skuCode: 'SKU-A',
    warehouseSkuCode: 'JHIN-SKU-A',
    skuName: '测试商品',
    receivedQuantity: 10,
    outerLengthMm: 100,
    outerWidthMm: 100,
    outerHeightMm: 100,
    outerGrossWeightG: 1000
  }],
  locations: [
    {
      locationId: 11,
      locationCode: 'A1-01',
      zoneName: '标准区',
      zoneType: 'STANDARD',
      capacityCalculable: true,
      capacityVolumeMm3: 20_000_000,
      occupiedVolumeMm3: 0,
      occupiedWeightGrams: 0,
      maxWeightGrams: 20_000,
      skuKindCount: 0,
      maxSkuKinds: 4
    },
    {
      locationId: 12,
      locationCode: 'D1-01',
      zoneName: '不良品区',
      zoneType: 'DEFECTIVE',
      capacityCalculable: false
    }
  ]
}

describe('putaway Excel template', () => {
  it('creates one prefilled row for every received SKU', () => {
    expect(createTemplateRows(context)).toEqual([{
      inboundNo: 'JHIN-RK-001',
      warehouseSkuCode: 'JHIN-SKU-A',
      skuName: '测试商品',
      receivedQuantity: 10,
      quality: '良品',
      locationCode: '',
      quantity: undefined,
      capacityOverrideReason: ''
    }])
  })

  it('round-trips the dedicated workbook without losing prefilled rows', async () => {
    const buffer = await buildPutawayTemplateBuffer(context)
    const rows = await parsePutawayTemplateBuffer(buffer)
    expect(rows).toEqual(createTemplateRows(context))
  }, 60_000)

  it('accepts one SKU split across multiple valid location rows', () => {
    const result = validateTemplateRows([
      { inboundNo: 'JHIN-RK-001', warehouseSkuCode: 'JHIN-SKU-A', skuName: '测试商品', receivedQuantity: 10, quality: '良品', locationCode: 'A1-01', quantity: 6, capacityOverrideReason: '' },
      { inboundNo: 'JHIN-RK-001', warehouseSkuCode: 'JHIN-SKU-A', skuName: '测试商品', receivedQuantity: 10, quality: '良品', locationCode: 'A1-01', quantity: 4, capacityOverrideReason: '' }
    ], context)
    expect(result.errors).toEqual([])
    expect(result.lines).toHaveLength(2)
    expect(result.lines[0]).toMatchObject({ skuCode: 'SKU-A', locationId: 11, quantity: 6, quality: 'GOOD' })
  })

  it('rejects the whole import when order, location, quality or totals are invalid', () => {
    const result = validateTemplateRows([
      { inboundNo: 'OTHER', warehouseSkuCode: 'JHIN-SKU-A', skuName: '测试商品', receivedQuantity: 10, quality: '不良品', locationCode: 'A1-99', quantity: 8, capacityOverrideReason: '' }
    ], context)
    expect(result.lines).toEqual([])
    expect(result.errors.join('\n')).toContain('入库单号与当前入库单不一致')
    expect(result.errors.join('\n')).toContain('库位编码不存在')
    expect(result.errors.join('\n')).toContain('模板合计 8 件，实收 10 件')
  })

  it('requires a reason when imported placement exceeds volume or weight', () => {
    const result = validateTemplateRows([
      { inboundNo: 'JHIN-RK-001', warehouseSkuCode: 'JHIN-SKU-A', skuName: '测试商品', receivedQuantity: 10, quality: '良品', locationCode: 'A1-01', quantity: 10, capacityOverrideReason: '' }
    ], {
      ...context,
      locations: [{ ...context.locations[0], capacityVolumeMm3: 5_000_000 }]
    })
    expect(result.lines).toEqual([])
    expect(result.errors.join('\n')).toContain('体积或承重超限，必须填写超限说明')
  })
})
