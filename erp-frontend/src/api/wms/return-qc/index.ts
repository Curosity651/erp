import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type { ReturnOrderVO, ReturnQO, ReturnReceiveDTO, ReturnQcDTO } from './types'
import { mockPage, mockDetail, mockReceive, mockQc } from './mock'

/**
 * 是否使用 mock 数据。
 * 后端退货执行接口就绪后置 false 即切真实接口；稳定后可删 mock.ts 与本开关。
 *
 * 后端需实现的接口（前缀建议 /wms/return-qc）：
 *   GET   /page          分页 待收货/待质检/已完成 退货单  query=PageParam+ReturnQO → PageResult<ReturnOrderVO>
 *   GET   /{id}          退货单详情(含 items)             → ReturnOrderVO
 *   POST  /receive       退货收货 RETURN_PENDING→QC_PENDING  body=ReturnReceiveDTO → void
 *   POST  /qc            质检+上架 QC_PENDING→COMPLETED      body=ReturnQcDTO → void
 *                        (PASS→退货区/标准区 GOOD 生成新批次；FAIL→不良品区 DAMAGED；FAIL+电子类校验照片)
 */
const USE_MOCK = false

const BASE = '/wms/return-qc'

function ok<T>(data: T): ApiResult<T> {
  return { code: 200, data, message: 'ok' }
}
const delay = () => new Promise(resolve => setTimeout(resolve, 260))

/** 分页：待收货 / 待质检 / 已完成 退货单 */
export async function pageReturns(
  pageParam: PageParam,
  qo: ReturnQO
): Promise<ApiResult<PageResult<ReturnOrderVO>>> {
  if (USE_MOCK) {
    await delay()
    const page = Number(pageParam.page ?? 1)
    const size = Number(pageParam.size ?? 10)
    return ok(mockPage(page, size, qo))
  }
  return httpClient.get(`${BASE}/page`, { params: { ...pageParam, ...qo } })
}

/** 退货单详情（含 items） */
export async function getReturnDetail(id: number): Promise<ApiResult<ReturnOrderVO>> {
  if (USE_MOCK) {
    await delay()
    const d = mockDetail(id)
    return d ? ok(d) : { code: 404, data: null as any, message: '退货单不存在' }
  }
  return httpClient.get(`${BASE}/${id}`)
}

/** 退货收货 */
export async function receiveReturn(dto: ReturnReceiveDTO): Promise<ApiResult<void>> {
  if (USE_MOCK) {
    await delay()
    const r = mockReceive(dto)
    return r.ok ? ok(undefined as any) : { code: 500, data: null as any, message: r.message }
  }
  return httpClient.post(`${BASE}/receive`, dto)
}

/** 未收到或拒收，关闭待收货退货单并释放申报额度。 */
export async function closeReturn(id: number): Promise<ApiResult<void>> {
  return httpClient.post(`${BASE}/${id}/close`)
}

/** 质检 + 上架 */
export async function submitQc(dto: ReturnQcDTO): Promise<ApiResult<void>> {
  if (USE_MOCK) {
    await delay()
    const r = mockQc(dto)
    return r.ok ? ok(undefined as any) : { code: 500, data: null as any, message: r.message }
  }
  return httpClient.post(`${BASE}/qc`, dto)
}

/** 按退货单所属仓库+分区查可用(未占用)库位，供质检上架库位下拉 */
export async function getAvailableLocations(
  returnOrderId: number,
  zone: string
): Promise<ApiResult<string[]>> {
  return httpClient.get(`${BASE}/available-locations`, { params: { returnOrderId, zone } })
}
