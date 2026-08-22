import test from 'node:test'
import assert from 'node:assert/strict'
import {
  aggregateReceivedGoods,
  buildReceivedSkuLabelDocument,
  waitForImageElements
} from './received-goods-print.ts'

test('aggregateReceivedGoods uses backend SKU code and combines duplicate rows', () => {
  const result = aggregateReceivedGoods([
    {
      skuCode: 'SKU-1',
      warehouseSkuCode: 'OWNER01-SKU-1',
      actualQuantity: 2,
      skuBrief: { skuCode: 'SKU-1', skuName: 'Product one', mainImage: 'https://example.com/1.jpg' }
    },
    {
      skuCode: 'SKU-1',
      warehouseSkuCode: 'OWNER01-SKU-1',
      actualQuantity: 3,
      skuBrief: { skuCode: 'SKU-1', skuName: 'Product one' }
    },
    {
      skuCode: 'SKU-2',
      warehouseSkuCode: 'OWNER01-SKU-2',
      actualQuantity: 0,
      skuBrief: { skuCode: 'SKU-2', skuName: 'Not printable' }
    }
  ])

  assert.deepEqual(result, [
    {
      originalSkuCode: 'SKU-1',
      warehouseSkuCode: 'OWNER01-SKU-1',
      skuName: 'Product one',
      mainImage: 'https://example.com/1.jpg',
      actualQuantity: 5
    }
  ])
})

test('waitForImageElements waits until a pending image loads', async () => {
  let onLoad: (() => void) | undefined
  const image = {
    complete: false,
    addEventListener(type: string, listener: () => void) {
      if (type === 'load') onLoad = listener
    }
  }

  let finished = false
  const pending = waitForImageElements([image]).then(() => {
    finished = true
  })
  await Promise.resolve()
  assert.equal(finished, false)
  onLoad?.()
  await pending
  assert.equal(finished, true)
})

test('buildReceivedSkuLabelDocument creates one compact Code 128 label per received box', () => {
  const html = buildReceivedSkuLabelDocument([
    {
      item: {
        originalSkuCode: 'ADNZ-004-QXM-02',
        warehouseSkuCode: 'JHIN-ADNZ-004-QXM-02',
        skuName: 'Product name must not be printed',
        actualQuantity: 2
      },
      barcodeDataUrl: 'data:image/png;base64,code128'
    }
  ])

  assert.match(html, /@page \{ size: 50mm 25mm; margin: 0; \}/)
  assert.equal((html.match(/<section class="label">/g) || []).length, 2)
  assert.equal((html.match(/class="barcode"/g) || []).length, 2)
  assert.equal((html.match(/JHIN-ADNZ-004-QXM-02/g) || []).length, 4)
  assert.doesNotMatch(html, />ADNZ-004-QXM-02<\/div>/)
  assert.doesNotMatch(html, /Product name must not be printed/)
  assert.doesNotMatch(html, /第 1 \/ 2 件/)
  assert.doesNotMatch(html, /class="qr"/)
})
