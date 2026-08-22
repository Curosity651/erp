import test from 'node:test'
import assert from 'node:assert/strict'
import { warehouseFulfillmentBlockReason } from './fulfillment-confirm.ts'

test('active warehouse fulfillment blocks duplicate submission', () => {
  assert.equal(
    warehouseFulfillmentBlockReason({ warehouseFulfillmentStatus: 'WAITING_PICK' }),
    '仓库履约已提交（WAITING_PICK）'
  )
})

test('cancelled warehouse fulfillment cannot be submitted again', () => {
  assert.equal(
    warehouseFulfillmentBlockReason({ warehouseFulfillmentStatus: 'CANCELLED' }),
    '仓库履约已取消，不允许再次提交'
  )
})

test('order without warehouse fulfillment remains eligible', () => {
  assert.equal(warehouseFulfillmentBlockReason({}), '')
})
