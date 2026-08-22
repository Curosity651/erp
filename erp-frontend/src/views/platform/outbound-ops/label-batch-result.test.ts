import test from 'node:test'
import assert from 'node:assert/strict'
import { describeLabelBatchResult } from './label-batch-result.ts'

test('generated batch is reported as success', () => {
  assert.deepEqual(
    describeLabelBatchResult({ status: 'GENERATED', successCount: 3, failedCount: 0 }),
    { level: 'success', text: '平台面单已生成，共 3 单' }
  )
})

test('partial batch reports both success and failure counts', () => {
  assert.deepEqual(
    describeLabelBatchResult({ status: 'PARTIAL', successCount: 2, failedCount: 1 }),
    { level: 'warning', text: '面单部分生成成功：成功 2 单，失败 1 单' }
  )
})

test('failed batch is never reported as success', () => {
  assert.deepEqual(
    describeLabelBatchResult({ status: 'FAILED', successCount: 0, failedCount: 3 }),
    { level: 'error', text: '面单生成失败，共 3 单' }
  )
})
