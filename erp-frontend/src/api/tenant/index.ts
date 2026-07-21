import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  TenantIdentity,
  OpenTenantParam,
  TenantBrief,
  TenantPageParam
} from '@/api/tenant/types'

/**
 * 获取当前登录用户的身份，可选校验登录入口是否匹配。
 * @param expectType 前端选择的入口类型（OVERSEAS_PLATFORM/WMS_OPERATOR/ERP_USER），不传则不校验
 */
export function getCurrentTenantIdentity(expectType?: string) {
  return httpClient.get<ApiResult<TenantIdentity>>('/tenant/current', {
    params: { expectType }
  })
}

/** 平台超管开通 WMS 服务商 */
export function openWmsOperator(data: OpenTenantParam) {
  return httpClient.post<ApiResult<number>>('/tenant/wms-operators', data)
}

/** WMS 服务商列表（平台超管，全量，下拉等用） */
export function listWmsOperators() {
  return httpClient.get<ApiResult<TenantBrief[]>>('/tenant/wms-operators')
}

/** WMS 服务商分页（平台超管） */
export function pageWmsOperators(params: TenantPageParam) {
  return httpClient.get<ApiResult<PageResult<TenantBrief>>>('/tenant/wms-operators/page', {
    params
  })
}

/** 启用/停用 WMS 服务商（平台超管，停用级联其名下货主） */
export function setWmsOperatorStatus(id: number, status: number) {
  return httpClient.put<ApiResult<void>>(`/tenant/wms-operators/${id}/status`, null, {
    params: { status }
  })
}

/** 某 WMS 服务商名下货主（平台超管，详情下钻） */
export function listErpTenantsByParent(id: number) {
  return httpClient.get<ApiResult<TenantBrief[]>>(`/tenant/wms-operators/${id}/erp-tenants`)
}

/** WMS 服务商开通货主 */
export function openErpTenant(data: OpenTenantParam) {
  return httpClient.post<ApiResult<number>>('/tenant/erp-tenants', data)
}

/** 货主列表（当前 WMS 服务商名下） */
export function listErpTenants() {
  return httpClient.get<ApiResult<TenantBrief[]>>('/tenant/erp-tenants')
}

/** 全部货主列表（平台超管，含所属服务商，报废单等平台侧下拉用） */
export function listAllErpTenants() {
  return httpClient.get<ApiResult<TenantBrief[]>>('/tenant/all-erp-tenants')
}

/** 货主分页（当前 WMS 服务商名下） */
export function pageErpTenants(params: TenantPageParam) {
  return httpClient.get<ApiResult<PageResult<TenantBrief>>>('/tenant/erp-tenants/page', {
    params
  })
}

/** 启用/停用货主（当前服务商名下，仅控账号开通停用） */
export function setErpTenantStatus(id: number, status: number) {
  return httpClient.put<ApiResult<void>>(`/tenant/erp-tenants/${id}/status`, null, {
    params: { status }
  })
}
