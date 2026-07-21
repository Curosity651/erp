import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type {
  OutboundOrderVO,
  OutboundPickingQO,
  PickAllocationVO,
  PickDTO,
  PickListVO,
  PickerVO
} from './types'
import {
  mockPage,
  mockDetail,
  mockPreview,
  mockConfirmPick,
  mockPickList,
  mockPickers
} from './mock'

/**
 * 是否使用 mock 数据。
 * 后端出库执行接口就绪后置 false 即切真实接口；稳定后可删 mock.ts 与本开关。
 *
 * 后端需实现的接口（前缀建议 /wms/outbound-picking）：
 *   GET   /page                分页待下架/拣货中订单  query=PageParam+OutboundPickingQO → PageResult<OutboundOrderVO>
 *   GET   /{id}                出库单详情(含 items)   → OutboundOrderVO
 *   GET   /{id}/preview        FIFO 分配预览          → PickAllocationVO[]
 *   POST  /pick                确认下架(FIFO锁定/生成拣货单/转PICKING)  body=PickDTO → void
 *   GET   /{id}/pick-list      拣货单                 → PickListVO
 *   GET   /pickers             拣货员选项             → PickerVO[]
 */
const USE_MOCK = false

const BASE = '/wms/outbound-picking'

function ok<T>(data: T): ApiResult<T> {
  return { code: 200, data, message: 'ok' }
}
const delay = () => new Promise(resolve => setTimeout(resolve, 260))

/** 分页：待下架 / 拣货中 订单 */
export async function pagePickingOrders(
  pageParam: PageParam,
  qo: OutboundPickingQO
): Promise<ApiResult<PageResult<OutboundOrderVO>>> {
  if (USE_MOCK) {
    await delay()
    const page = Number(pageParam.page ?? 1)
    const size = Number(pageParam.size ?? 10)
    return ok(mockPage(page, size, qo))
  }
  return httpClient.get(`${BASE}/page`, { params: { ...pageParam, ...qo } })
}

/** 出库单详情（含 items） */
export async function getOutboundDetail(id: number): Promise<ApiResult<OutboundOrderVO>> {
  if (USE_MOCK) {
    await delay()
    const d = mockDetail(id)
    return d ? ok(d) : { code: 404, data: null as any, message: '订单不存在' }
  }
  return httpClient.get(`${BASE}/${id}`)
}

/** FIFO 分配预览 */
export async function previewAllocation(id: number): Promise<ApiResult<PickAllocationVO[]>> {
  if (USE_MOCK) {
    await delay()
    return ok(mockPreview(id))
  }
  return httpClient.get(`${BASE}/${id}/preview`)
}

/** 确认下架 */
export async function confirmPick(dto: PickDTO): Promise<ApiResult<void>> {
  if (USE_MOCK) {
    await delay()
    const r = mockConfirmPick(dto)
    return r.ok ? ok(undefined as any) : { code: 500, data: null as any, message: r.message }
  }
  return httpClient.post(`${BASE}/pick`, dto)
}

/** 拣货单 */
export async function getPickList(id: number): Promise<ApiResult<PickListVO>> {
  if (USE_MOCK) {
    await delay()
    const p = mockPickList(id)
    return p ? ok(p) : { code: 404, data: null as any, message: '拣货单不存在' }
  }
  return httpClient.get(`${BASE}/${id}/pick-list`)
}

/** 拣货员选项 */
export async function listPickers(): Promise<ApiResult<PickerVO[]>> {
  if (USE_MOCK) {
    await delay()
    return ok(mockPickers())
  }
  return httpClient.get(`${BASE}/pickers`)
}
