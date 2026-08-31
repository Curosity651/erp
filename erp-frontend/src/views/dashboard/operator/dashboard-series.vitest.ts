import { describe, expect, it } from 'vitest'
import { buildTrendSeries } from './dashboard-series'

describe('operator dashboard currency series', () => {
  it('keeps income and expense series separated by currency', () => {
    const series = buildTrendSeries({
      months: ['2026-07', '2026-08'],
      incomeSeries: [
        { currency: 'CNY', amounts: [10, 20] },
        { currency: 'RUB', amounts: [100, 200] }
      ],
      confirmedExpenseSeries: [{ currency: 'CNY', amounts: [3, 4] }],
      paidExpenseSeries: [{ currency: 'CNY', amounts: [1, 2] }]
    })

    expect(series.map(item => item.name)).toEqual([
      'CNY 收入',
      'RUB 收入',
      'CNY 已确认应付',
      'CNY 已付款'
    ])
    expect(series[0].data).toEqual([10, 20])
    expect(series[1].data).toEqual([100, 200])
  })
})
