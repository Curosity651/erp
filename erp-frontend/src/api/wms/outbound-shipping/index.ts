import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type {
  PackShipOrderVO,
  PackShipQO,
  PackDTO,
  ShipDTO,
  ShipResultVO,
  LogisticsChannelVO
} from './types'
import { mockPage, mockDetail, mockPack, mockShip, mockChannels } from './mock'

/**
 * 是否使用 mock 数据。
 * 后端出库执行接口就绪后置 false 即切真实接口；稳定后可删 mock.ts 与本开关。
 *
 * 后端需实现的接口（前缀建议 /wms/outbound-shipping）：
 *   GET   /page              分页 拣货中/已打包/已发货 订单  query=PageParam+PackShipQO → PageResult<PackShipOrderVO>
 *   GET   /{id}              订单详情(含 items)             → PackShipOrderVO
 *   POST  /pack              打包 PICKING→PACKED(打印标签)  body=PackDTO → void
 *   POST  /ship              签出 PACKED→SHIPPED            body=ShipDTO → ShipResultVO
 *                            (扣物理库存+释放 reserved，生成链路二物流费 wms_client_billing_record)
 *   GET   /channels          物流渠道选项                   → LogisticsChannelVO[]
 */
const USE_MOCK = false

const BASE = '/wms/outbound-shipping'

function ok<T>(data: T): ApiResult<T> {
  return { code: 200, data, message: 'ok' }
}
const delay = () => new Promise(resolve => setTimeout(resolve, 260))

/** 分页：拣货中 / 已打包 / 已发货 订单 */
export async function pagePackShip(
  pageParam: PageParam,
  qo: PackShipQO
): Promise<ApiResult<PageResult<PackShipOrderVO>>> {
  if (USE_MOCK) {
    await delay()
    const page = Number(pageParam.page ?? 1)
    const size = Number(pageParam.size ?? 10)
    return ok(mockPage(page, size, qo))
  }
  return httpClient.get(`${BASE}/page`, { params: { ...pageParam, ...qo } })
}

/** 订单详情（含 items） */
export async function getPackShipDetail(id: number): Promise<ApiResult<PackShipOrderVO>> {
  if (USE_MOCK) {
    await delay()
    const d = mockDetail(id)
    return d ? ok(d) : { code: 404, data: null as any, message: '订单不存在' }
  }
  return httpClient.get(`${BASE}/${id}`)
}

/** 打包 */
export async function confirmPack(dto: PackDTO): Promise<ApiResult<void>> {
  if (USE_MOCK) {
    await delay()
    const r = mockPack(dto)
    return r.ok ? ok(undefined as any) : { code: 500, data: null as any, message: r.message }
  }
  return httpClient.post(`${BASE}/pack`, dto)
}

/** 签出 */
export async function confirmShip(dto: ShipDTO): Promise<ApiResult<ShipResultVO>> {
  if (USE_MOCK) {
    await delay()
    const r = mockShip(dto)
    return r.ok && r.data ? ok(r.data) : { code: 500, data: null as any, message: r.message }
  }
  return httpClient.post(`${BASE}/ship`, dto)
}

/** 物流渠道选项 */
export async function listChannels(): Promise<ApiResult<LogisticsChannelVO[]>> {
  if (USE_MOCK) {
    await delay()
    return ok(mockChannels())
  }
  return httpClient.get(`${BASE}/channels`)
}
