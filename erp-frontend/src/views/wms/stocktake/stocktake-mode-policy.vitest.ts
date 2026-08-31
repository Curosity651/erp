import { describe, expect, it } from 'vitest'
import { creatableStocktakeModes } from './stocktake-mode-policy'

describe('stocktake creation modes', () => {
  it('only exposes full and cycle stocktakes', () => {
    expect(creatableStocktakeModes.map(mode => mode.value)).toEqual(['FULL', 'CYCLE'])
  })
})
