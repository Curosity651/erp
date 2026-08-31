import { describe, expect, it, vi } from 'vitest'
import { createFailureAlertState } from './failure-alert'

describe('failure alert state', () => {
  it('automatically clears failures after the display duration', () => {
    vi.useFakeTimers()
    const state = createFailureAlertState(10_000)

    state.show(['7: 平台未返回该订单的处理结果'])
    expect(state.failures.value).toHaveLength(1)

    vi.advanceTimersByTime(10_000)
    expect(state.failures.value).toEqual([])
    state.dispose()
    vi.useRealTimers()
  })

  it('can be dismissed immediately', () => {
    vi.useFakeTimers()
    const state = createFailureAlertState(10_000)

    state.show(['failed'])
    state.clear()

    expect(state.failures.value).toEqual([])
    vi.advanceTimersByTime(10_000)
    expect(state.failures.value).toEqual([])
    state.dispose()
    vi.useRealTimers()
  })
})
