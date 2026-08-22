import { describe, expect, it } from 'vitest'
import { buildReceivedGoodsReferenceDocument } from './received-goods-print'

describe('received goods reference print layout', () => {
  it('keeps all columns inside the printable width', () => {
    const html = buildReceivedGoodsReferenceDocument(
      {
        inboundNo: 'JHIN-TEST-IN-20260817-003',
        ownerName: 'JHIN',
        warehouseName: '李安'
      },
      [
        {
          originalSkuCode: 'ADNZ-012-SXM-02',
          warehouseSkuCode: 'JHIN-ADNZ-012-SXM-02',
          skuName: 'L型增高架木制抽屉电脑桌',
          actualQuantity: 4
        }
      ]
    )

    expect(html).toContain('<colgroup>')
    expect(html).toContain('class="col-image"')
    expect(html).toContain('class="col-quantity"')
    expect(html).toContain('overflow-wrap: anywhere')
    expect(html).toContain('@media screen')
    expect(html).toContain('padding: 12px')
  })
})
