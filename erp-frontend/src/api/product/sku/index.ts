import httpClient from '@/utils/axios'
import type { ApiResult, PageResult, PageParam } from '@/api/types'
import type {
  SkuCreateDTO,
  SkuUpdateDTO,
  SkuQO,
  SkuPageVO,
  SkuDetailVO,
  SkuOverviewStats,
  SkuFilterOptions,
  SkuSearchParam,
  SkuStatsData,
  SkuSelectQO,
  SkuSelectVO
} from './types'

// ==================== 基础CRUD操作 ====================

/**
 * SKU管理表分页查询
 * @param pageParams 分页参数
 * @param queryParams 查询条件
 */
export function pageSku(pageParams: PageParam, queryParams?: SkuQO) {
  return httpClient.post<ApiResult<PageResult<SkuPageVO>>>('/product/sku/page', queryParams || {}, {
    params: pageParams
  })
}

/**
 * 根据ID查询SKU详情
 * @param id SKU ID
 */
export function getSkuDetail(id: number) {
  return httpClient.get<ApiResult<SkuDetailVO>>('/product/sku/detail', {
    params: { id }
  })
}

/**
 * 创建SKU
 * @param dto SKU创建数据
 */
export function createSku(dto: SkuCreateDTO) {
  return httpClient.post<ApiResult<void>>('/product/sku/save', dto)
}

/**
 * 修改SKU
 * @param dto SKU更新数据
 */
export function updateSku(dto: SkuUpdateDTO) {
  return httpClient.post<ApiResult<void>>('/product/sku/update', dto)
}

/**
 * 删除SKU
 * @param id 主键ID
 */
export function deleteSku(id: number) {
  return httpClient.post<ApiResult<void>>('/product/sku/delete', null, {
    params: { id }
  })
}

// ==================== 业务相关接口 ====================

/**
 * 验证SKU编码唯一性
 * @param code SKU编码
 * @param excludeId 排除的ID（更新时使用）
 */
export function validateSkuCode(code: string, excludeId?: number) {
  return httpClient.get<ApiResult<boolean>>('/product/sku/validate-code', {
    params: { code, excludeId }
  })
}

/**
 * 验证SKU序号唯一性
 * @param skuNo SKU序号
 * @param excludeId 排除的ID（更新时使用）
 */
export function validateSkuNo(skuNo: number, excludeId?: number) {
  return httpClient.get<ApiResult<boolean>>('/product/sku/validate-no', {
    params: { skuNo, excludeId }
  })
}

/**
 * 根据SKU编码查询SKU
 * @param code SKU编码
 */
export function getSkuByCode(code: string) {
  return httpClient.get<ApiResult<SkuDetailVO>>('/product/sku/by-code', {
    params: { code }
  })
}

// ==================== 高级查询接口 ====================

/**
 * 高级搜索分页查询SKU
 * @param pageParams 分页参数
 * @param searchParam 搜索参数
 */
export function advancedSearchSku(pageParams: PageParam, searchParam?: SkuSearchParam) {
  return httpClient.get<ApiResult<PageResult<SkuPageVO>>>('/product/sku/advanced-search', {
    params: { ...pageParams, ...searchParam }
  })
}

/**
 * 关键字搜索分页查询SKU
 * @param pageParams 分页参数
 * @param keyword 关键字
 */
export function searchSkuByKeyword(pageParams: PageParam, keyword: string) {
  return httpClient.get<ApiResult<PageResult<SkuPageVO>>>('/product/sku/search', {
    params: { ...pageParams, keyword }
  })
}

/**
 * 多条件复合查询分页
 * @param pageParams 分页参数
 * @param searchParam 搜索参数
 */
export function querySkuByMultipleConditions(pageParams: PageParam, searchParam: SkuSearchParam) {
  return httpClient.get<ApiResult<PageResult<SkuPageVO>>>('/product/sku/multi-search', {
    params: { ...pageParams, ...searchParam }
  })
}

/**
 * 智能搜索分页查询SKU
 * @param pageParams 分页参数
 * @param searchText 搜索文本
 */
export function smartSearchSku(pageParams: PageParam, searchText: string) {
  return httpClient.get<ApiResult<PageResult<SkuPageVO>>>('/product/sku/smart-search', {
    params: { ...pageParams, searchText }
  })
}

// ==================== 统计分析接口 ====================

/**
 * 统计各状态的SKU数量
 */
export function getSkuCountByStatus() {
  return httpClient.get<ApiResult<SkuStatsData[]>>('/product/sku/stats/by-status')
}

/**
 * 统计各品牌的SKU数量
 */
export function getSkuCountByBrand() {
  return httpClient.get<ApiResult<SkuStatsData[]>>('/product/sku/stats/by-brand')
}

/**
 * 统计各供应商的SKU数量
 */
export function getSkuCountBySupplier() {
  return httpClient.get<ApiResult<SkuStatsData[]>>('/product/sku/stats/by-supplier')
}

/**
 * 获取SKU数据概览统计
 */
export function getSkuOverviewStats() {
  return httpClient.get<ApiResult<SkuOverviewStats>>('/product/sku/overview-stats')
}

// ==================== 筛选选项接口 ====================

/**
 * 获取筛选选项数据
 */
export function getSkuFilterOptions() {
  return httpClient.get<ApiResult<SkuFilterOptions>>('/product/sku/filter-options')
}

/**
 * 获取所有品牌列表（去重）
 */
export function getDistinctBrands() {
  return httpClient.get<ApiResult<string[]>>('/product/sku/brands')
}

/**
 * 获取所有项目组列表（去重）
 */
export function getDistinctProjectGroups() {
  return httpClient.get<ApiResult<string[]>>('/product/sku/project-groups')
}

/**
 * 获取所有供应商编码列表（去重）
 */
export function getDistinctSupplierCodes() {
  return httpClient.get<ApiResult<string[]>>('/product/sku/supplier-codes')
}

/**
 * 获取所有销售国家列表（去重）
 */
export function getDistinctSalesCountries() {
  return httpClient.get<ApiResult<string[]>>('/product/sku/sales-countries')
}

// ==================== 批量操作接口 ====================

/**
 * 批量更新产品状态
 * @param skuIds SKU ID列表
 * @param productStatus 新的产品状态
 */
export function batchUpdateProductStatus(skuIds: number[], productStatus: number) {
  return httpClient.post<ApiResult<number>>('/product/sku/batch-update-status', skuIds, {
    params: { productStatus }
  })
}

/**
 * 导出SKU数据
 * @param searchParam 查询条件
 */
export function exportSkus(searchParam?: SkuSearchParam) {
  return httpClient.get<ApiResult<SkuPageVO[]>>('/product/sku/export', {
    params: searchParam
  })
}

/**
 * 导出SKU数据（POST，请求体传参，返回文件流）
 * 注意：服务端返回的是Excel文件流，需要在调用处用 remoteFileDownload 触发下载
 */
export function exportSkusExcelByPost(searchParam?: unknown) {
  return httpClient.post('/product/sku/export', searchParam || {}, {
    responseType: 'blob'
  })
}

// ==================== SKU选择弹窗专用接口 ====================

/**
 * SKU选择弹窗分页查询（轻量级）
 * @param pageParams 分页参数
 * @param queryParams 查询条件
 */
export function pageSkuForSelect(pageParams: PageParam, queryParams?: SkuSelectQO) {
  return httpClient.post<ApiResult<PageResult<SkuSelectVO>>>(
    '/product/sku/select-page',
    queryParams || {},
    {
      params: pageParams
    }
  )
}

/**
 * 根据SKU编码批量查询（用于选择弹窗回显）
 * @param skuCodes SKU编码列表，最多100条
 */
export function listSkuByCodes(skuCodes: string[]) {
  // 前端校验最大数量
  if (skuCodes.length > 100) {
    return Promise.reject(new Error('SKU编码数量不能超过100条'))
  }
  return httpClient.post<ApiResult<SkuSelectVO[]>>('/product/sku/list-by-codes', skuCodes)
}
