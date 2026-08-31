import type { TrendVO } from '@/api/wms/operator-dashboard/types'

export interface DashboardSeries {
  name: string
  type: 'bar' | 'line'
  data: number[]
  smooth?: boolean
}

export function buildTrendSeries(trend: TrendVO): DashboardSeries[] {
  return [
    ...trend.incomeSeries.map(item => ({
      name: `${item.currency} 收入`,
      type: 'bar' as const,
      data: item.amounts
    })),
    ...trend.confirmedExpenseSeries.map(item => ({
      name: `${item.currency} 已确认应付`,
      type: 'line' as const,
      data: item.amounts,
      smooth: true
    })),
    ...trend.paidExpenseSeries.map(item => ({
      name: `${item.currency} 已付款`,
      type: 'line' as const,
      data: item.amounts,
      smooth: true
    }))
  ]
}
