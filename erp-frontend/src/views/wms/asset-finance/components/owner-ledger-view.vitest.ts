import { describe, expect, it } from 'vitest'
import { currentMonthKey, defaultExpandedMonths } from './owner-ledger-view'

describe('owner ledger monthly view', () => {
  it('keeps every month collapsed by default', () => {
    expect(defaultExpandedMonths([
      { month: '2026-08' },
      { month: '2026-07' }
    ], new Date('2026-08-26T12:00:00+08:00'))).toEqual([])
  })

  it('returns no expanded row when the current month has no entries', () => {
    expect(defaultExpandedMonths([{ month: '2026-07' }], new Date('2026-08-26T12:00:00+08:00'))).toEqual([])
  })

  it('formats the local current month key', () => {
    expect(currentMonthKey(new Date(2026, 7, 26))).toBe('2026-08')
  })
})
