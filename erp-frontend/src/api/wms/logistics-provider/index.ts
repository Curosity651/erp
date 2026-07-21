import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  LogisticsProviderDTO,
  LogisticsProviderOptionVO,
  LogisticsProviderPageParam,
  LogisticsProviderPageVO
} from './types'

/**
 * 物流商分页查询
 * @param pageParams 分页参数
 */
export function pageLogisticsProvider(pageParams: LogisticsProviderPageParam) {
  return httpClient.get<ApiResult<LogisticsProviderPageVO>>('/wms/logistics-provider/page', {
    params: pageParams
  })
}

/**
 * 创建物流商
 * @param dto 物流商数据传输对象
 */
export function createLogisticsProvider(dto: LogisticsProviderDTO) {
  return httpClient.post<ApiResult<void>>('/wms/logistics-provider', dto)
}

/**
 * 修改物流商
 * @param dto 物流商数据传输对象
 */
export function updateLogisticsProvider(dto: LogisticsProviderDTO) {
  return httpClient.put<ApiResult<void>>('/wms/logistics-provider', dto)
}

/**
 * 获取物流商下拉选项列表
 */
export function getLogisticsProviderOptions() {
  return httpClient.get<ApiResult<LogisticsProviderOptionVO[]>>('/wms/logistics-provider/options')
}

/**
 * 更新物流商状态
 * @param id 物流商ID
 * @param status 状态: 1-启用 / 0-停用
 */
export function updateLogisticsProviderStatus(id: number, status: number) {
  return httpClient.patch<ApiResult<void>>('/wms/logistics-provider/status', { id, status })
}

/**
 * 获取物流商详情
 * @param id 物流商ID
 */
export function getLogisticsProviderDetail(id: number) {
  return httpClient.get<ApiResult<LogisticsProviderPageVO>>('/wms/logistics-provider/detail', {
    params: { id }
  })
}
