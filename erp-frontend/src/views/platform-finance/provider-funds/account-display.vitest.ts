import { describe, expect, it } from 'vitest'
import { formatAccountTime } from './account-display'

describe('provider settlement account display', () => {
  it('formats real account changes without the ISO T separator', () => {
    expect(formatAccountTime('2026-08-25T20:00:14')).toBe('2026-08-25 20:00:14')
  })

  it('does not expose the Unix epoch as a business change time', () => {
    expect(formatAccountTime('1970-01-01T00:00:00')).toBe('--')
    expect(formatAccountTime(undefined)).toBe('--')
  })
})
