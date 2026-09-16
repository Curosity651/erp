import { describe, expect, it } from 'vitest'
import { applyResolvedReturnSkus, buildReturnReceiptPayload } from './return-receipt-flow'

describe('return receipt global SKU draft flow', () => {
  const original = [
    {
      key: 1,
      warehouseSkuCode: ' jhin-sku-1 ',
      receivedQty: 2,
      returnReason: 'OTHER',
      photoFileIds: [],
      uploading: false
    }
  ]

  it('applies canonical SKU and display-only product identity', () => {
    const result = applyResolvedReturnSkus(original, [
      {
        requestedCode: 'jhin-sku-1',
        warehouseSkuCode: 'JHIN-SKU-1',
        matched: true,
        erpTenantId: 6,
        ownerName: 'JHIN',
        skuCode: 'SKU-1',
        skuName: '测试商品'
      }
    ])

    expect(result.errors).toEqual([])
    expect(result.lines[0]).toMatchObject({
      warehouseSkuCode: 'JHIN-SKU-1',
      ownerName: 'JHIN',
      originalSkuCode: 'SKU-1',
      skuName: '测试商品',
      matched: true
    })
    expect(original[0].warehouseSkuCode).toBe(' jhin-sku-1 ')
  })

  it('keeps the original draft unchanged when any SKU is unmatched', () => {
    const result = applyResolvedReturnSkus(original, [
      {
        requestedCode: 'jhin-sku-1',
        warehouseSkuCode: 'jhin-sku-1',
        matched: false,
        error: '全局 SKU 不存在'
      }
    ])

    expect(result.errors).toEqual(['第 1 行：全局 SKU 不存在'])
    expect(result.lines).toEqual(original)
    expect(result.lines).not.toBe(original)
  })

  it('builds a receipt payload with global SKU as the only product identity', () => {
    const payload = buildReturnReceiptPayload(
      { warehouseId: 9, returnDate: '2026-09-16', remark: ' test ' },
      [
        {
          ...original[0],
          warehouseSkuCode: ' JHIN-SKU-1 ',
          platformOrderId: ' OZON-1 ',
          photoFileIds: [10, 11]
        }
      ]
    )

    expect(payload.items[0]).toEqual({
      warehouseSkuCode: 'JHIN-SKU-1',
      receivedQty: 2,
      platformOrderId: 'OZON-1',
      returnReason: 'OTHER',
      photoFileIds: [10, 11]
    })
    expect(payload.items[0]).not.toHaveProperty('erpTenantId')
    expect(payload.items[0]).not.toHaveProperty('skuCode')
  })
})
