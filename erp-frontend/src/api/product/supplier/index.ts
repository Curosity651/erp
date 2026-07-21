import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { SupplierDTO, SupplierPageParam, SupplierPageVO } from './types'

/**
 * 供应商分页查询
 * @param pageParams 分页参数
 */
export function pageSupplier(pageParams: SupplierPageParam) {
  return httpClient.get<ApiResult<SupplierPageVO>>('/product/supplier/page', {
    params: pageParams
  })
}

/**
 * 创建供应商
 * @param dto
 */
export function createSupplier(dto: SupplierDTO) {
  return httpClient.post<ApiResult<void>>('/product/supplier', dto)
}

/**
 * 修改供应商
 * @param dto
 */
export function updateSupplier(dto: SupplierDTO) {
  return httpClient.put<ApiResult<void>>('/product/supplier', dto)
}

/**
 * 删除供应商
 * @param id 主键ID
 */
export function deleteSupplier(id: number) {
  return httpClient.delete<ApiResult<void>>(`/product/supplier/` + id)
}

/** Get a short-lived preview URL for the supplier's private business license image. */
export function getBusinessLicenseUrl(id: number) {
  return httpClient.get<ApiResult<string>>(`/product/supplier/${id}/business-license-url`)
}

/**
 * 获取供应商选项列表（用于下拉选择）
 * @param keyword 搜索关键词（可选）
 */
export function getSupplierOptions(keyword?: string) {
  return httpClient.get<ApiResult<SupplierPageVO[]>>('/product/supplier/options', {
    params: {
      keyword
    }
  })
}
