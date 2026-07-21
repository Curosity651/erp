import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { PlatformDashboardQueryParams, PlatformDashboardDataVO } from './types'
import { buildMockPlatformDashboard } from './mock'

/**
 * 是否使用 mock 数据。
 * 后端 /api/platform-dashboard/data 就绪后，置为 false 即切到真实接口；
 * 稳定后可删除 mock.ts 与本开关。
 */
const USE_MOCK = false

/**
 * 获取平台数据分析看板数据（仓储运营视角，跨全部货主聚合）。
 */
export async function getPlatformDashboardData(
  params: PlatformDashboardQueryParams
): Promise<PlatformDashboardDataVO> {
  if (USE_MOCK) {
    // 模拟网络延迟，贴近真实加载体验
    await new Promise(resolve => setTimeout(resolve, 300))
    return buildMockPlatformDashboard(params)
  }
  // 响应拦截器已解包一层为 ApiResult（见 utils/axios onResponseFulfilled 返回 response.data），
  // 故 res 即 ApiResult，业务数据在 res.data，切勿再多取一层（此前误写 res.data.data → undefined，
  // 导致看板全部板块拿到 undefined 而显示 0/暂无数据）。
  const res = await httpClient.post<ApiResult<PlatformDashboardDataVO>>(
    '/api/platform-dashboard/data',
    params
  )
  return res.data
}
