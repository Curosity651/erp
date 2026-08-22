import test from 'node:test'
import assert from 'node:assert/strict'
import { printPutawayReceipt } from './putaway-receipt-print.ts'
import { printPalletLabels } from '../pallet/pallet-label-print.ts'

function printWindow() {
  let html = ''
  const page = {
    document: {
      open() {},
      close() {},
      write(value: string) {
        html += value
      }
    },
    focus() {},
    print() {}
  }
  return { page, html: () => html }
}

test('putaway receipt prints the stable warehouse SKU code', () => {
  const output = printWindow()
  globalThis.window = {
    open: () => output.page,
    setTimeout(callback: () => void) {
      callback()
      return 1
    }
  } as never

  printPutawayReceipt(
    {
      id: 1,
      inboundNo: 'JHIN-RK-1',
      shippingOrderId: 1,
      shippingOrderNo: 'WL-1',
      warehouseId: 1,
      warehouseName: 'Warehouse',
      inboundDate: '2026-07-29',
      orderStatus: 'COMPLETED',
      itemSummary: '',
      createTime: '2026-07-29'
    },
    [{
      slotCode: 'A1-L1-P01',
      skuCode: 'SKU-1',
      warehouseSkuCode: 'JHIN-SKU-1',
      quality: 'GOOD',
      quantity: 2
    }]
  )

  assert.match(output.html(), />JHIN-SKU-1</)
})

test('pallet label prints the stable warehouse SKU code', async () => {
  const output = printWindow()
  globalThis.window = {
    setTimeout(callback: () => void) {
      callback()
      return 1
    }
  } as never

  await printPalletLabels([{
    id: 1,
    palletNo: 'PLT-1',
    warehouseId: 1,
    palletType: 'MIXED',
    palletStatus: 'PARTIAL',
    capacitySource: 'MANUAL',
    skuKindCount: 1,
    wholePalletEligible: 0,
    createTime: '2026-07-29',
    items: [{
      erpTenantId: 6,
      skuCode: 'SKU-1',
      warehouseSkuCode: 'JHIN-SKU-1',
      quantity: 2,
      reservedQty: 0,
      quality: 'GOOD'
    }]
  }], output.page as never)

  assert.match(output.html(), />JHIN-SKU-1 × 2</)
})
