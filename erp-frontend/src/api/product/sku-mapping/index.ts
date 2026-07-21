import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type {
  SkuMappingDTO,
  SkuMappingPageParam,
  SkuMappingPageVO,
  SkuMappingListItem,
  SkuMappingQuickCreateDTO
} from './types'

/**
 * SKU映射分页查询
 * @param pageParams 分页参数
 */
export function pageSkuMapping(pageParams: SkuMappingPageParam) {
  return httpClient.get<ApiResult<SkuMappingPageVO>>('/product/sku-mapping/page', {
    params: pageParams
  })
}

/**
 * 创建SKU映射
 * @param dto
 */
export function createSkuMapping(dto: SkuMappingDTO) {
  return httpClient.post<ApiResult<void>>('/product/sku-mapping', dto)
}

/**
 * 修改SKU映射
 * @param dto
 */
export function updateSkuMapping(dto: SkuMappingDTO) {
  return httpClient.put<ApiResult<void>>('/product/sku-mapping', dto)
}

/**
 * 删除SKU映射
 * @param id 主键ID
 */
export function deleteSkuMapping(id: number) {
  return httpClient.delete<ApiResult<void>>(`/product/sku-mapping/` + id)
}

/**
 * 导出SKU映射（使用浏览器下载）
 */
export function exportSkuMapping(params: Record<string, any>) {
  return httpClient
    .get<Blob>('/product/sku-mapping/export', {
      params,
      responseType: 'blob'
    })
    .then(res => {
      // 简单下载逻辑
      const blob = new Blob([res as any])
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'sku-mapping.xlsx'
      a.click()
      window.URL.revokeObjectURL(url)
    })
}

/**
 * 根据SKU编码查询映射列表
 * @param skuCode SKU编码
 */
export function getSkuMappingsBySkuCode(skuCode: string) {
  return httpClient.get<ApiResult<SkuMappingListItem[]>>('/product/sku-mapping/by-sku-code', {
    params: { skuCode }
  })
}

/**
 * 快速创建映射
 * @param dto 映射数据
 */
export function quickCreateSkuMapping(dto: SkuMappingQuickCreateDTO) {
  return httpClient.post<ApiResult<void>>('/product/sku-mapping/quick-create', dto)
}
