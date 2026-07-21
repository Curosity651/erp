import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  ShopListQuery,
  ShopVO,
  ShopDetailVO,
  TestCredentialRequest,
  TestCredentialResponse,
  CreateOrUpdateShopRequest
} from './types'

// ===== Shop endpoints (canonical) =====

// 列表分页查询（简化返回结构）
export function listShops(query: ShopListQuery) {
  return httpClient.get<ApiResult<PageResult<ShopVO>>>('/shops', {
    params: query
  })
}

// 获取单个店铺（含遮罩后的凭证信息）
export function getShop(id: number) {
  return httpClient.get<ApiResult<ShopDetailVO>>(`/shops/${id}`)
}

// 测试凭证
export function testCredential(req: TestCredentialRequest) {
  return httpClient.post<ApiResult<TestCredentialResponse>>('/shops/test', req)
}

// 创建店铺（使用测试成功后的 token 防止绕过）
export function createShop(req: CreateOrUpdateShopRequest) {
  return httpClient.post<ApiResult<{ id: number }>>('/shops', req)
}

// 更新店铺（重新测试后方可提交）
export function updateShop(id: number, req: CreateOrUpdateShopRequest) {
  return httpClient.put<ApiResult<void>>(`/shops/${id}`, req)
}

// 启用/禁用
export function toggleShopStatus(id: number, status: number) {
  return httpClient.patch<ApiResult<void>>(`/shops/${id}/status`, { status })
}

// 重测店铺
export async function retestShop(id: number) {
  return httpClient.post(`/shops/${id}/test`)
}

// 已移除旧 /shop/shop* 兼容接口
