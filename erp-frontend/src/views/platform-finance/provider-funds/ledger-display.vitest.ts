import { describe, expect, it } from 'vitest'
import { ledgerStatementMeta } from './ledger-display'

describe('ledgerStatementMeta', () => {
  it('shows current month as realtime rather than a draft bill', () => {
    expect(ledgerStatementMeta('REALTIME')).toEqual({ text: '实时统计', color: 'blue' })
  })
})
