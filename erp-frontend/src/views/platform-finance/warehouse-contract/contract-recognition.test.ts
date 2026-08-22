import test from 'node:test'
import assert from 'node:assert/strict'
import { contractMonths, recognizedMonthValues } from './contract-recognition.ts'

test('contractMonths only returns months covered by the contract term', () => {
  assert.deepEqual(contractMonths('2026-03-15', '2026-06-02'), [
    { value: '2026-03', label: '2026年03月' },
    { value: '2026-04', label: '2026年04月' },
    { value: '2026-05', label: '2026年05月' },
    { value: '2026-06', label: '2026年06月' }
  ])
})

test('recognizedMonthValues only accepts posted monthly service recognition rows', () => {
  const result = recognizedMonthValues([
    {
      accountingMonth: '2026-03',
      transactionType: 'RECOGNITION',
      fundComponent: 'SUBSCRIPTION_SERVICE',
      direction: 'IN',
      status: 'POSTED'
    },
    {
      accountingMonth: '2026-04',
      transactionType: 'RECEIPT',
      fundComponent: 'SUBSCRIPTION_SERVICE',
      direction: 'IN',
      status: 'POSTED'
    },
    {
      accountingMonth: '2026-05',
      transactionType: 'RECOGNITION',
      fundComponent: 'SUBSCRIPTION_SERVICE',
      direction: 'IN',
      status: 'VOID'
    }
  ])

  assert.deepEqual(result, ['2026-03'])
})
