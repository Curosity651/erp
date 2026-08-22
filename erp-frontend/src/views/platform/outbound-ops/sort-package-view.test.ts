import test from 'node:test'
import assert from 'node:assert/strict'
import { buildSortItemRows } from './sort-package-view.ts'

test('buildSortItemRows exposes internal SKU and remaining quantity', () => {
  assert.deepEqual(
    buildSortItemRows([
      {
        skuCode: 'ADNZ-007-QXM-02',
        warehouseSkuCode: 'JHIN-ADNZ-007-QXM-02',
        skuName: '测试商品',
        qty: 3,
        sortedQty: 1,
        packedQty: 0,
        quality: 'GOOD'
      }
    ]),
    [
      {
        skuCode: 'ADNZ-007-QXM-02',
        warehouseSkuCode: 'JHIN-ADNZ-007-QXM-02',
        skuName: '测试商品',
        requiredQty: 3,
        sortedQty: 1,
        remainingQty: 2
      }
    ]
  )
})

test('buildSortItemRows never returns a negative remaining quantity', () => {
  const [row] = buildSortItemRows([
    {
      skuCode: 'SKU-1',
      qty: 1,
      sortedQty: 2,
      packedQty: 0,
      quality: 'GOOD'
    }
  ])

  assert.equal(row.remainingQty, 0)
})
