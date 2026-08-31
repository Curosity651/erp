import { describe, expect, it } from 'vitest'
import { billOperations } from './bill-operation-policy'

describe('billOperations', () => {
  it('allows adjustments and review only before the bill is reviewed', () => {
    expect(billOperations('DRAFT')).toEqual({ view: true, adjust: true, review: true })
    expect(billOperations('DISPUTED')).toEqual({ view: true, adjust: true, review: true })
    expect(billOperations('CONFIRMED')).toEqual({ view: true, adjust: false, review: false })
    expect(billOperations('PAID')).toEqual({ view: true, adjust: false, review: false })
  })
})
