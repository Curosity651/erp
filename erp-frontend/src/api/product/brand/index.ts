import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { BrandDTO, BrandListVO, BrandPageParam, BrandPageVO } from './types'

/**
 * 品牌管理表分页查询
 * @param pageParams 分页参数
 */
export function pageBrand(pageParams: BrandPageParam) {
  return httpClient.get<ApiResult<BrandPageVO>>('/product/brand/page', {
    params: pageParams
  })
}

/**
 * 创建品牌管理表
 * @param dto
 */
export function createBrand(dto: BrandDTO) {
  return httpClient.post<ApiResult<void>>('/product/brand', dto)
}

/**
 * 修改品牌管理表
 * @param dto
 */
export function updateBrand(dto: BrandDTO) {
  return httpClient.put<ApiResult<void>>('/product/brand', dto)
}

/**
 * 删除品牌管理表
 * @param id 主键ID
 */
export function deleteBrand(id: number) {
  return httpClient.delete<ApiResult<void>>(`/product/brand/` + id)
}

/**
 * 查询品牌管理
 */
export function getBrandList() {
  return httpClient.get<ApiResult<BrandListVO[]>>('/product/brand/list')
}
