import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  SalesTargetDTO,
  SalesTargetPageParam,
  SalesTargetPageVO,
  SalesTargetBatchCreateDTO,
  SalesTargetYearlyOverviewVO,
  SalesTargetBatchMonthlyUpdateDTO,
  SalesTargetUpdateDTO
} from './types'

/**
 * 销售目标分页查询
 * @param pageParams 分页参数
 */
export function pageSalesTarget(pageParams: SalesTargetPageParam) {
  return httpClient.get<ApiResult<SalesTargetPageVO>>('/system/sales-target/page', {
    params: pageParams
  })
}

/**
 * 创建销售目标
 * @param dto
 */
export function createSalesTarget(dto: SalesTargetDTO) {
  return httpClient.post<ApiResult<void>>('/system/sales-target', dto)
}

/**
 * 修改销售目标
 * @param dto
 */
export function updateSalesTarget(dto: SalesTargetDTO) {
  return httpClient.put<ApiResult<void>>('/system/sales-target', dto)
}

/**
 * 删除销售目标
 * @param id 主键ID
 */
export function deleteSalesTarget(id: number) {
  return httpClient.delete<ApiResult<void>>(`/system/sales-target/` + id)
}

/**
 * 批量创建年度和月度目标
 * @param dto 批量创建DTO
 */
export function batchCreateSalesTarget(dto: SalesTargetBatchCreateDTO) {
  return httpClient.post<ApiResult<void>>('/system/sales-target/batch', dto)
}

/**
 * 获取年度概览
 * @param year 目标年份
 */
export function getYearlyOverview(year: number) {
  return httpClient.get<ApiResult<SalesTargetYearlyOverviewVO>>(
    `/system/sales-target/yearly/${year}`
  )
}

/**
 * 批量更新月度目标
 * @param dto 批量更新DTO
 */
export function batchUpdateMonthly(dto: SalesTargetBatchMonthlyUpdateDTO) {
  return httpClient.put<ApiResult<void>>('/system/sales-target/batch-monthly', dto)
}

/**
 * 更新单个月度目标
 * @param id 目标ID
 * @param dto 更新DTO
 */
export function updateSingleMonthly(id: number, dto: SalesTargetUpdateDTO) {
  return httpClient.put<ApiResult<void>>(`/system/sales-target/monthly/${id}`, dto)
}

/**
 * 创建单个月度目标
 * @param dto 月度目标DTO
 */
export function createSingleMonthly(dto: MonthlyTargetInputDTO & { year: number }) {
  return httpClient.post<ApiResult<void>>('/system/sales-target/monthly', dto)
}

/**
 * 创建年度目标
 * @param dto 年度目标DTO
 */
export function createAnnualTarget(dto: { year: number; amount: number; remark?: string }) {
  return httpClient.post<ApiResult<void>>('/system/sales-target/annual', dto)
}

/**
 * 更新年度目标
 * @param id 目标ID
 * @param dto 更新DTO
 */
export function updateAnnualTarget(id: number, dto: SalesTargetUpdateDTO) {
  return httpClient.put<ApiResult<void>>(`/system/sales-target/annual/${id}`, dto)
}
