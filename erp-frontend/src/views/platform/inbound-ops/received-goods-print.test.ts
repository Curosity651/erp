import test from 'node:test'
import assert from 'node:assert/strict'
import {
  aggregateReceivedGoods,
  buildWarehouseSkuCode
} from './received-goods-print.ts'

test('buildWarehouseSkuCode normalizes owner name and preserves original SKU', () => {
  assert.equal(buildWarehouseSkuCode(' jhin ', ' ADNZ-015-TXW '), 'JHIN-ADNZ-015-TXW')
})

test('aggregateReceivedGoods combines duplicate SKU rows by actual quantity', () => {
  const result = aggregateReceivedGoods('JHIN', [
    {
      skuCode: 'SKU-1',
      actualQuantity: 2,
      skuBrief: { skuCode: 'SKU-1', skuName: '商品一', mainImage: 'https://example.com/1.jpg' }
    },
    {
      skuCode: 'SKU-1',
      actualQuantity: 3,
      skuBrief: { skuCode: 'SKU-1', skuName: '商品一' }
    },
    {
      skuCode: 'SKU-2',
      actualQuantity: 0,
      skuBrief: { skuCode: 'SKU-2', skuName: '不应打印' }
    }
  ])

  assert.deepEqual(result, [
    {
      originalSkuCode: 'SKU-1',
      warehouseSkuCode: 'JHIN-SKU-1',
      skuName: '商品一',
      mainImage: 'https://example.com/1.jpg',
      actualQuantity: 5
    }
  ])
})
