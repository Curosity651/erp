import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { CategoryDTO, CategoryPageParam, CategoryPageVO } from './types'

/**
 * 商品品类分页查询
 * @param pageParams 分页参数
 */
export function pageCategory(pageParams: CategoryPageParam) {
  return httpClient.get<ApiResult<CategoryPageVO>>('/product/category/page', {
    params: pageParams
  })
}

/**
 * 查询所有商品品类（用于树形结构）
 * @param params 查询参数
 */
export function listCategory(params?: any) {
  return httpClient.get<ApiResult<CategoryPageVO[]>>('/product/category/list', {
    params
  })
}

/**
 * 创建商品品类
 * @param dto
 */
export function createCategory(dto: CategoryDTO) {
  return httpClient.post<ApiResult<void>>('/product/category', dto)
}

/**
 * 修改商品品类
 * @param dto
 */
export function updateCategory(dto: CategoryDTO) {
  return httpClient.put<ApiResult<void>>('/product/category', dto)
}

/**
 * 删除商品品类
 * @param id 主键ID
 */
export function deleteCategory(id: number) {
  return httpClient.delete<ApiResult<void>>(`/product/category/` + id)
}
