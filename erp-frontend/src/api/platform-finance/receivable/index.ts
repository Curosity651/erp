import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'
import type {
  MonthlyBillVO,
  MonthlyBillQO,
  BillStatus,
  GenerateBillDTO,
  GenerateBillResultVO
} from './types'
import { mockPageBills, mockGetBill, mockChangeStatus, mockGenerate } from './mock'

/**
 * 是否使用 mock 数据。
 * 后端 wms_monthly_bill 接口就绪后置 false 即切真实接口；稳定后可删 mock.ts 与本开关。
 *
 * 后端需实现的接口（前缀建议 /platform-finance/monthly-bill）：
 *   POST  /page                分页查询       body=MonthlyBillQO, query=PageParam → PageResult<MonthlyBillVO>
 *   GET   /{id}                详情           → MonthlyBillVO
 *   POST  /generate            生成/重算      body=GenerateBillDTO → GenerateBillResultVO
 *   POST  /{id}/confirm        草稿→已确认（争议→已确认 亦复用此端点）
 *   POST  /{id}/pay            已确认→已付款
 *   POST  /{id}/dispute        已确认→争议    body={ remark }
 */
const USE_MOCK = false

const BASE = '/platform-finance/monthly-bill'

function ok<T>(data: T): ApiResult<T> {
  return { code: 200, data, message: 'ok' }
}

const delay = () => new Promise(resolve => setTimeout(resolve, 260))

/** 分页查询应收账单 */
export async function pageMonthlyBill(
  pageParam: PageParam,
  qo: MonthlyBillQO
): Promise<ApiResult<PageResult<MonthlyBillVO>>> {
  if (USE_MOCK) {
    await delay()
    const page = Number(pageParam.page ?? 1)
    const size = Number(pageParam.size ?? 10)
    return ok(mockPageBills(page, size, qo))
  }
  return httpClient.post(`${BASE}/page`, qo, { params: pageParam })
}

/** 账单详情 */
export async function getMonthlyBillDetail(id: number): Promise<ApiResult<MonthlyBillVO>> {
  if (USE_MOCK) {
    await delay()
    const b = mockGetBill(id)
    return b ? ok(b) : { code: 404, data: null as any, message: '账单不存在' }
  }
  return httpClient.get(`${BASE}/${id}`)
}

/** 生成 / 重算账单 */
export async function generateMonthlyBill(
  dto: GenerateBillDTO
): Promise<ApiResult<GenerateBillResultVO>> {
  if (USE_MOCK) {
    await delay()
    return ok(mockGenerate(dto))
  }
  return httpClient.post(`${BASE}/generate`, dto)
}

/** 状态流转（confirm / pay / dispute / reconfirm 统一走这里） */
export async function changeBillStatus(
  id: number,
  target: BillStatus,
  remark?: string
): Promise<ApiResult<MonthlyBillVO>> {
  if (USE_MOCK) {
    await delay()
    const b = mockChangeStatus(id, target)
    return b ? ok(b) : { code: 404, data: null as any, message: '账单不存在' }
  }
  const path = target === 'PAID' ? 'pay' : target === 'DISPUTED' ? 'dispute' : 'confirm'
  return httpClient.post(`${BASE}/${id}/${path}`, { remark })
}
