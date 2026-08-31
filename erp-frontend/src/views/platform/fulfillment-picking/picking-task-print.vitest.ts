import { describe, expect, it } from 'vitest'
import { buildPickingTaskPrintHtml } from './picking-task-print'

describe('fulfillment picking task print', () => {
  it('prints task summary and groups lines by order and owner', () => {
    const html = buildPickingTaskPrintHtml({
      taskNo: 'PT-001',
      warehouseName: '合作仓库',
      printedAt: '2026-08-23 20:30:00',
      orderCount: 2,
      totalQuantity: 3,
      orders: [
        {
          orderNo: 'ORDER-001',
          ownerName: 'JHIN',
          sourceType: 'OZON',
          lines: [
            {
              locationCode: 'A1-01',
              warehouseSkuCode: 'JHIN-SKU-1',
              skuCode: 'SKU-1',
              plannedQuantity: 2
            }
          ]
        },
        {
          orderNo: 'ORDER-002',
          ownerName: '第二货主',
          sourceType: 'WB',
          lines: [
            {
              locationCode: 'A1-02',
              warehouseSkuCode: 'OWNER-SKU-2',
              skuCode: 'SKU-2',
              plannedQuantity: 1
            }
          ]
        }
      ]
    })

    expect(html).toContain('拣货任务号：PT-001')
    expect(html).toContain('仓库：合作仓库')
    expect(html).toContain('订单号：ORDER-001')
    expect(html).toContain('货主：JHIN')
    expect(html).toContain('订单号：ORDER-002')
    expect(html).toContain('货主：第二货主')
    expect(html).toContain('商品 SKU')
  })

  it('escapes business text before writing the print window', () => {
    const html = buildPickingTaskPrintHtml({
      taskNo: '<script>alert(1)</script>',
      warehouseName: 'A&B',
      printedAt: '2026-08-23',
      orderCount: 0,
      totalQuantity: 0,
      orders: []
    })

    expect(html).not.toContain('<script>alert(1)</script>')
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
    expect(html).toContain('A&amp;B')
  })
})
