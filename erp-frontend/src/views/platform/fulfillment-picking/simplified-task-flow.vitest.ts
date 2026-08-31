import { describe, expect, it } from 'vitest'
import { canCompleteSimplifiedTask, canOpenSimplifiedTask } from './simplified-task-flow'

describe('simplified task flow', () => {
  it('keeps scan and simplified task modes mutually exclusive', () => {
    expect(canOpenSimplifiedTask('PICKING', undefined, 99, 99)).toBe(true)
    expect(canOpenSimplifiedTask('PICKING', 'SIMPLE', 99, 99)).toBe(true)
    expect(canOpenSimplifiedTask('PICKING', 'SCAN', 99, 99)).toBe(false)
    expect(canOpenSimplifiedTask('PICKING', undefined, 88, 99)).toBe(false)
  })

  it('requires evidence and every active order label before completion', () => {
    expect(canCompleteSimplifiedTask(1, [
      { orderStatus: 'PENDING', labelReady: true },
      { orderStatus: 'CANCELLED', labelReady: false }
    ])).toBe(true)
    expect(canCompleteSimplifiedTask(0, [
      { orderStatus: 'PENDING', labelReady: true }
    ])).toBe(false)
    expect(canCompleteSimplifiedTask(1, [
      { orderStatus: 'PENDING', labelReady: false }
    ])).toBe(false)
    expect(canCompleteSimplifiedTask(1, [
      { orderStatus: 'EXCEPTION', labelReady: true }
    ])).toBe(false)
  })
})
