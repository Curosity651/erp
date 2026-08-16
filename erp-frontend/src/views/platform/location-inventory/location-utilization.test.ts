import test from 'node:test'
import assert from 'node:assert/strict'
import { calculateUtilization, utilizationLevel } from './location-utilization.ts'

test('calculateUtilization returns zero when capacity is unavailable', () => {
  assert.equal(calculateUtilization(300, 0), 0)
  assert.equal(calculateUtilization(300, undefined), 0)
})

test('calculateUtilization rounds normal usage and caps visual fill at 100', () => {
  assert.equal(calculateUtilization(333, 1000), 33.3)
  assert.equal(calculateUtilization(1200, 1000), 100)
})

test('utilizationLevel keeps capacity states visually distinct', () => {
  assert.equal(utilizationLevel(20), 'low')
  assert.equal(utilizationLevel(70), 'medium')
  assert.equal(utilizationLevel(92), 'high')
})
