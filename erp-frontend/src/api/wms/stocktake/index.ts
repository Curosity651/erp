import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type { PalletSummaryVO } from '@/api/wms/pallet'
import type {
  StocktakeDTO,
  StocktakeItemsDTO,
  StocktakePageParam,
  StocktakePageVO,
  StocktakeDetailVO,
  StocktakeItemVO,
  StocktakeLocationTaskVO,
  StocktakeExtraItemDTO,
  StocktakeAddSkuDTO,
  AvailableSkuVO
} from './types'

export function getStocktakeTasks(id: number) {
  return httpClient.get<ApiResult<StocktakeLocationTaskVO[]>>('/wms/stocktake/tasks', {
    params: { id }
  })
}

export function getStocktakeTaskItems(taskId: number) {
  return httpClient.get<ApiResult<StocktakeItemVO[]>>('/wms/stocktake/task/items', {
    params: { taskId }
  })
}

export function getStocktakeEligibleOwnerIds(taskId: number) {
  return httpClient.get<ApiResult<number[]>>('/wms/stocktake/task/eligible-owners', {
    params: { taskId }
  })
}

export function addStocktakeExtraItem(dto: StocktakeExtraItemDTO) {
  return httpClient.post<ApiResult<StocktakeItemVO>>('/wms/stocktake/task/extra', dto)
}

export function completeStocktakeTask(taskId: number) {
  return httpClient.patch<ApiResult<void>>('/wms/stocktake/task/complete', null, {
    params: { taskId }
  })
}

export function submitStocktakeReview(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/stocktake/review', null, {
    params: { id }
  })
}

/**
 * 盘点单分页查询
 * @param params 分页参数
 */
export function pageStocktake(params: StocktakePageParam) {
  return httpClient.get<ApiResult<PageResult<StocktakePageVO>>>('/wms/stocktake/page', {
    params
  })
}

/**
 * 获取盘点单详情
 * @param id 盘点单ID
 */
export function getStocktakeDetail(id: number) {
  return httpClient.get<ApiResult<StocktakeDetailVO>>('/wms/stocktake/detail', {
    params: { id }
  })
}

export function getStocktakePallets(id: number) {
  return httpClient.get<ApiResult<PalletSummaryVO[]>>('/wms/stocktake/pallets', {
    params: { id }
  })
}

/**
 * 创建盘点单
 * @param dto 盘点单数据传输对象
 */
export function createStocktake(dto: StocktakeDTO) {
  return httpClient.post<ApiResult<number>>('/wms/stocktake', dto)
}

/**
 * 获取盘点明细列表
 * @param id 盘点单ID
 */
export function getStocktakeItems(id: number) {
  return httpClient.get<ApiResult<StocktakeItemVO[]>>('/wms/stocktake/items', {
    params: { id }
  })
}

/**
 * 录入盘点数据
 * @param dto 盘点录入数据传输对象
 */
export function saveStocktakeItems(dto: StocktakeItemsDTO) {
  return httpClient.put<ApiResult<void>>('/wms/stocktake/items', dto)
}

/**
 * 确认盘点（直接触发 STOCKTAKE 过账）
 * @param id 盘点单ID
 */
export function confirmStocktake(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/stocktake/confirm', null, {
    params: { id }
  })
}

/**
 * 取消盘点
 * @param id 盘点单ID
 */
export function cancelStocktake(id: number) {
  return httpClient.patch<ApiResult<void>>('/wms/stocktake/cancel', null, {
    params: { id }
  })
}

/**
 * 删除盘点单
 * @param ids 盘点单ID列表
 */
export function deleteStocktake(ids: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/stocktake', {
    data: ids
  })
}

/**
 * 获取差异预览
 * @param id 盘点单ID
 */
export function getDiffPreview(id: number) {
  return httpClient.get<ApiResult<import('./types').StocktakeDiffPreviewVO>>(
    '/wms/stocktake/diff-preview',
    {
      params: { id }
    }
  )
}

/**
 * 批量标记无差异
 * @param id 盘点单ID
 */
export function batchMarkNoDiff(id: number) {
  return httpClient.put<ApiResult<void>>('/wms/stocktake/batch-no-diff', null, {
    params: { id }
  })
}

/**
 * 获取仓库可盘点SKU列表（预览）
 * @param warehouseId 仓库ID
 */
export function getAvailableSkuPreview(warehouseId: number) {
  return httpClient.get<ApiResult<import('./types').AvailableSkuPreviewVO>>(
    '/wms/stocktake/available-skus',
    {
      params: { warehouseId }
    }
  )
}

/**
 * 获取仓库内可选择的SKU列表（用于部分盘点选择）
 * @param warehouseId 仓库ID
 * @param stocktakeId 盘点单ID（可选，用于排除已添加的）
 */
export function getSelectableSkus(
  warehouseId: number,
  stocktakeId?: number,
  erpTenantId?: number
) {
  return httpClient.get<ApiResult<AvailableSkuVO[]>>('/wms/stocktake/selectable-skus', {
    params: { warehouseId, stocktakeId, erpTenantId }
  })
}

/**
 * 追加盘点SKU
 * @param dto 追加请求
 * @returns 新增的明细列表
 */
export function addStocktakeSkus(dto: StocktakeAddSkuDTO) {
  return httpClient.post<ApiResult<StocktakeItemVO[]>>('/wms/stocktake/add-skus', dto)
}

/**
 * 删除盘点明细
 * @param stocktakeId 盘点单ID
 * @param itemIds 明细ID列表
 */
export function removeStocktakeItems(stocktakeId: number, itemIds: number[]) {
  return httpClient.delete<ApiResult<void>>('/wms/stocktake/items', {
    params: { stocktakeId },
    data: itemIds
  })
}
