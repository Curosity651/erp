import { describe, expect, it } from 'vitest'
import type { OutboundPickReviewSummary } from '@/api/wms/fulfillment/types'
import * as flow from './workbench-flow'

const { failedOrderIds } = flow

describe('fulfillment workbench flow', () => {
  it('keeps only failed orders selected after batch signout', () => {
    expect(failedOrderIds({ 12: '缺少费率', 15: '库存已变化' })).toEqual([12, 15])
  })

  it('allows selecting only orders waiting to be shelved', () => {
    expect(flow.isShelfOrderSelectable).toBeTypeOf('function')
    expect(flow.isShelfOrderSelectable?.('WAITING_SHELF')).toBe(true)
    expect(flow.isShelfOrderSelectable?.('WAITING_PICK')).toBe(false)
    expect(flow.isShelfOrderSelectable?.('PICKING')).toBe(false)
  })

  it('supplies clearly identified demo tasks and matching summary for an empty development page', () => {
    const result = flow.resolveOutboundWorkbenchData?.({
      devMode: true,
      workDate: '2026-09-16',
      warehouseId: 7,
      tasks: [],
      summary: emptySummary('2026-09-16', 7)
    })

    expect(result).toBeDefined()
    expect(result?.demoMode).toBe(true)
    expect(result?.tasks).toHaveLength(5)
    expect(result?.tasks.map(task => task.taskStatus)).toEqual([
      'PENDING',
      'PICKING',
      'PARTIAL_EXCEPTION',
      'COMPLETED',
      'CANCELLED'
    ])
    expect(result?.summary).toMatchObject({
      totalTaskCount: 5,
      completedTaskCount: 1,
      cancelledTaskCount: 1,
      pendingTaskCount: 1,
      pickingTaskCount: 1,
      exceptionTaskCount: 1,
      unprocessedTaskCount: 3,
      allProcessed: false,
      reviewCurrent: false
    })
    expect(Object.keys(result?.details || {})).toHaveLength(5)
  })

  it('never replaces real data or enables demo data outside development', () => {
    const summary = emptySummary('2026-09-16', 7)
    const realTask = {
      id: 88,
      taskNo: 'PT-REAL-88',
      warehouseId: 7,
      taskStatus: 'COMPLETED' as const,
      orderCount: 1,
      totalQuantity: 2
    }

    const withRealTask = flow.resolveOutboundWorkbenchData?.({
      devMode: true,
      workDate: '2026-09-16',
      warehouseId: 7,
      tasks: [realTask],
      summary: { ...summary, totalTaskCount: 1 }
    })
    const productionEmpty = flow.resolveOutboundWorkbenchData?.({
      devMode: false,
      workDate: '2026-09-16',
      warehouseId: 7,
      tasks: [],
      summary
    })

    expect(withRealTask).toMatchObject({ demoMode: false, tasks: [realTask] })
    expect(productionEmpty).toMatchObject({ demoMode: false, tasks: [] })
  })

  it('applies task filters to demo rows while keeping the daily review summary', () => {
    const result = flow.resolveOutboundWorkbenchData?.({
      devMode: true,
      workDate: '2026-09-16',
      tasks: [],
      summary: emptySummary('2026-09-16', 7),
      taskFilter: { taskStatus: 'COMPLETED', operatorId: -9101 }
    })

    expect(result?.tasks.map(task => task.taskNo)).toEqual(['DEMO-PICK-20260916-004'])
    expect(result?.summary.totalTaskCount).toBe(5)
  })
})

function emptySummary(workDate: string, warehouseId: number): OutboundPickReviewSummary {
  return {
    workDate,
    warehouseId,
    totalTaskCount: 0,
    completedTaskCount: 0,
    cancelledTaskCount: 0,
    pendingTaskCount: 0,
    pickingTaskCount: 0,
    exceptionTaskCount: 0,
    unprocessedTaskCount: 0,
    allProcessed: false,
    reviewCurrent: false
  }
}
