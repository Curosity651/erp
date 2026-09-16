import { describe, expect, it } from 'vitest'
import { canCompleteSimplifiedTask, canOpenSimplifiedTask } from './simplified-task-flow'

describe('simplified task flow', () => {
  it('keeps scan and simplified task modes mutually exclusive', () => {
    expect(canOpenSimplifiedTask('PICKING', undefined, 99, 99)).toBe(true)
    expect(canOpenSimplifiedTask('PICKING', 'SIMPLE', 99, 99)).toBe(true)
    expect(canOpenSimplifiedTask('PICKING', 'SCAN', 99, 99)).toBe(false)
    expect(canOpenSimplifiedTask('PICKING', undefined, 88, 99)).toBe(false)
  })

  it('requires evidence and actionable orders but does not expose standalone label printing', () => {
    expect(canCompleteSimplifiedTask(1, [
      { orderStatus: 'PENDING' },
      { orderStatus: 'CANCELLED' }
    ])).toBe(true)
    expect(canCompleteSimplifiedTask(0, [
      { orderStatus: 'PENDING' }
    ])).toBe(false)
    expect(canCompleteSimplifiedTask(1, [
      { orderStatus: 'EXCEPTION' }
    ])).toBe(false)
  })
})
