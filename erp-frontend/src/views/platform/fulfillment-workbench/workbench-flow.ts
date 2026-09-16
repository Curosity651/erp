import type {
  FulfillmentPickTask,
  FulfillmentPickTaskDetail,
  FulfillmentPickTaskQuery,
  OutboundPickReviewSummary
} from '@/api/wms/fulfillment/types'

export function failedOrderIds(failures?: Record<string, string>) {
  return Object.keys(failures || {})
    .map(Number)
    .filter(Number.isFinite)
}

export function isShelfOrderSelectable(fulfillmentStatus?: string) {
  return fulfillmentStatus === 'WAITING_SHELF'
}

interface ResolveWorkbenchDataOptions {
  devMode: boolean
  workDate: string
  warehouseId?: number
  tasks: FulfillmentPickTask[]
  summary: OutboundPickReviewSummary
  taskFilter?: FulfillmentPickTaskQuery
}

interface ResolvedWorkbenchData {
  demoMode: boolean
  tasks: FulfillmentPickTask[]
  summary: OutboundPickReviewSummary
  details: Record<number, FulfillmentPickTaskDetail>
}

const demoOperatorNames: Record<number, string> = {
  [-9101]: '演示人员·李娜',
  [-9102]: '演示人员·王强'
}

export function demoOperatorName(operatorId?: number) {
  return operatorId ? demoOperatorNames[operatorId] : undefined
}

export function demoWarehouseName(warehouseId: number) {
  return warehouseId === -9001 ? '演示仓库' : undefined
}

export function resolveOutboundWorkbenchData(
  options: ResolveWorkbenchDataOptions
): ResolvedWorkbenchData {
  const { devMode, workDate, warehouseId, tasks, summary, taskFilter } = options
  if (!devMode || tasks.length > 0 || summary.totalTaskCount > 0) {
    return { demoMode: false, tasks, summary, details: {} }
  }

  const demoWarehouseId = warehouseId ?? -9001
  const compactDate = workDate.replaceAll('-', '')
  const demoTasks: FulfillmentPickTask[] = [
    {
      id: -1001,
      taskNo: `DEMO-PICK-${compactDate}-001`,
      warehouseId: demoWarehouseId,
      taskStatus: 'PENDING',
      orderCount: 3,
      totalQuantity: 12,
      completedOrderCount: 0,
      exceptionOrderCount: 0,
      createTime: `${workDate} 08:30:00`
    },
    {
      id: -1002,
      taskNo: `DEMO-PICK-${compactDate}-002`,
      warehouseId: demoWarehouseId,
      taskStatus: 'PICKING',
      orderCount: 4,
      totalQuantity: 18,
      operatorId: -9101,
      completedOrderCount: 1,
      exceptionOrderCount: 0,
      claimedTime: `${workDate} 09:05:00`,
      createTime: `${workDate} 08:40:00`
    },
    {
      id: -1003,
      taskNo: `DEMO-PICK-${compactDate}-003`,
      warehouseId: demoWarehouseId,
      taskStatus: 'PARTIAL_EXCEPTION',
      orderCount: 2,
      totalQuantity: 7,
      operatorId: -9102,
      completedOrderCount: 1,
      exceptionOrderCount: 1,
      claimedTime: `${workDate} 09:20:00`,
      createTime: `${workDate} 08:50:00`
    },
    {
      id: -1004,
      taskNo: `DEMO-PICK-${compactDate}-004`,
      warehouseId: demoWarehouseId,
      taskStatus: 'COMPLETED',
      orderCount: 5,
      totalQuantity: 25,
      operatorId: -9101,
      completedOrderCount: 5,
      exceptionOrderCount: 0,
      claimedTime: `${workDate} 08:55:00`,
      createTime: `${workDate} 08:20:00`
    },
    {
      id: -1005,
      taskNo: `DEMO-PICK-${compactDate}-005`,
      warehouseId: demoWarehouseId,
      taskStatus: 'CANCELLED',
      orderCount: 1,
      totalQuantity: 3,
      operatorId: -9102,
      completedOrderCount: 0,
      exceptionOrderCount: 0,
      claimedTime: `${workDate} 09:10:00`,
      createTime: `${workDate} 08:45:00`
    }
  ]

  const visibleTasks = demoTasks.filter(task => {
    if (taskFilter?.taskNo && !task.taskNo.includes(taskFilter.taskNo.trim())) return false
    if (taskFilter?.taskStatus && task.taskStatus !== taskFilter.taskStatus) return false
    if (taskFilter?.operatorId && task.operatorId !== taskFilter.operatorId) return false
    return true
  })

  return {
    demoMode: true,
    tasks: visibleTasks,
    summary: {
      workDate,
      warehouseId,
      totalTaskCount: 5,
      completedTaskCount: 1,
      cancelledTaskCount: 1,
      pendingTaskCount: 1,
      pickingTaskCount: 1,
      exceptionTaskCount: 1,
      unprocessedTaskCount: 3,
      allProcessed: false,
      reviewCurrent: false
    },
    details: Object.fromEntries(demoTasks.map(task => [task.id, createDemoDetail(task)]))
  }
}

function createDemoDetail(task: FulfillmentPickTask): FulfillmentPickTaskDetail {
  const orderStatus = {
    PENDING: 'PENDING',
    PICKING: 'PICKING',
    PARTIAL_EXCEPTION: 'EXCEPTION',
    COMPLETED: 'COMPLETED',
    CANCELLED: 'CANCELLED'
  }[task.taskStatus] as 'PENDING' | 'PICKING' | 'EXCEPTION' | 'COMPLETED' | 'CANCELLED'
  const fulfillmentStatus = {
    PENDING: 'WAITING_PICK',
    PICKING: 'PICKING',
    PARTIAL_EXCEPTION: 'EXCEPTION',
    COMPLETED: 'WAITING_PACK',
    CANCELLED: 'CANCELLED'
  }[task.taskStatus] as 'WAITING_PICK' | 'PICKING' | 'EXCEPTION' | 'WAITING_PACK' | 'CANCELLED'
  const quantities = Array.from(
    { length: task.orderCount },
    (_, index) =>
      Math.floor(task.totalQuantity / task.orderCount) +
      (index < task.totalQuantity % task.orderCount ? 1 : 0)
  )
  const orderQueue = quantities.map((quantity, index) => {
    const orderId = task.id * 100 - index
    const taskOrder = {
      id: orderId,
      fulfillmentOrderId: orderId,
      sequenceNo: index + 1,
      orderStatus,
      exceptionType:
        task.taskStatus === 'PARTIAL_EXCEPTION' && index === 1 ? 'STOCK_SHORTAGE' : undefined,
      exceptionReason:
        task.taskStatus === 'PARTIAL_EXCEPTION' && index === 1 ? '演示：库位库存不足' : undefined
    }
    const fulfillmentOrder = {
      id: orderId,
      erpTenantId: 1,
      warehouseId: task.warehouseId,
      fulfillmentNo: `DEMO-FO-${Math.abs(task.id)}-${index + 1}`,
      sourceType: index % 2 ? 'WB' : 'OZON',
      sourceOrderNo: `DEMO-${index % 2 ? 'WB' : 'OZON'}-${Math.abs(orderId)}`,
      fulfillmentStatus
    }
    const routeLines = [
      {
        id: orderId,
        fulfillmentOrderId: orderId,
        locationCode: `A-${String(index + 1).padStart(2, '0')}-01`,
        skuCode: `DEMO-SKU-${index + 1}`,
        warehouseSkuCode: `WH-DEMO-${index + 1}`,
        plannedQuantity: quantity,
        pickedQuantity:
          orderStatus === 'COMPLETED' ? quantity : index === 0 ? Math.min(quantity, 1) : 0,
        lineStatus: orderStatus
      }
    ]
    return {
      taskOrder,
      fulfillmentOrder,
      routeLines,
      firstLocationCode: routeLines[0].locationCode,
      skuCount: 1,
      totalQuantity: quantity,
      pickedQuantity: routeLines[0].pickedQuantity
    }
  })

  return {
    task,
    orders: orderQueue.map(item => item.taskOrder),
    lines: orderQueue.flatMap(item => item.routeLines),
    orderQueue
  }
}
