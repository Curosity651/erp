import { describe, expect, it, vi } from 'vitest'
import { createReactivationRefresh } from './reactivation-refresh'

describe('createReactivationRefresh', () => {
  it('skips the initial activation and refreshes whenever the cached page is reactivated', async () => {
    const refresh = vi.fn().mockResolvedValue(undefined)
    const onActivated = createReactivationRefresh(refresh)

    await onActivated()
    expect(refresh).not.toHaveBeenCalled()

    await onActivated()
    await onActivated()
    expect(refresh).toHaveBeenCalledTimes(2)
  })
})
