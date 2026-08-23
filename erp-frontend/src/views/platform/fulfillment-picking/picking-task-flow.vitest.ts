import { describe, expect, it } from 'vitest'
import { canReleaseTask, canSelectTaskOrder, primaryTaskAction } from './picking-task-flow'

describe('standalone fulfillment picking flow', () => {
  it('shows claim for pending tasks and work only to the claimant', () => {
    expect(primaryTaskAction('PENDING', undefined, 9)).toBe('claim')
    expect(primaryTaskAction('PICKING', 9, 9)).toBe('work')
    expect(primaryTaskAction('PICKING', 8, 9)).toBe('view')
    expect(canReleaseTask('PARTIAL_EXCEPTION', 9, 9)).toBe(true)
  })

  it('allows choosing any actionable order but not completed or cancelled orders', () => {
    expect(canSelectTaskOrder('PENDING')).toBe(true)
    expect(canSelectTaskOrder('PICKING')).toBe(true)
    expect(canSelectTaskOrder('WAITING_LABEL')).toBe(true)
    expect(canSelectTaskOrder('COMPLETED')).toBe(false)
    expect(canSelectTaskOrder('CANCELLED')).toBe(false)
  })
})
