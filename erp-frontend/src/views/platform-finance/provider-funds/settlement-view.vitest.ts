import { describe, expect, it } from 'vitest'
import { settlementViewPolicy } from './settlement-view'

describe('settlementViewPolicy', () => {
  it('keeps account and monthly bill filters isolated', () => {
    expect(settlementViewPolicy('account')).toEqual({ account: true, bill: false })
    expect(settlementViewPolicy('bill')).toEqual({ account: false, bill: true })
  })
})
