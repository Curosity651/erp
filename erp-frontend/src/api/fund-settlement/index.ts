import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { FundAccount, FundLedger, FundLedgerMonth, FundQuery, RechargeCreateForm, RechargeOrder } from './types'

const OWNER = '/fund-settlement/owner'
const OPERATOR = '/fund-settlement/operator'
const PLATFORM = '/fund-settlement/platform'

export const getOwnerFundAccounts = () => httpClient.get<ApiResult<FundAccount[]>>(`${OWNER}/accounts`)
export const getOwnerRecharges = (params?: FundQuery) => httpClient.get<ApiResult<RechargeOrder[]>>(`${OWNER}/recharges`, { params })
export const getOwnerLedger = (params?: FundQuery) => httpClient.get<ApiResult<FundLedger[]>>(`${OWNER}/ledger`, { params })
export const getOwnerLedgerMonths = (params?: FundQuery) => httpClient.get<ApiResult<FundLedgerMonth[]>>(`${OWNER}/ledger/months`, { params })
export const exportOwnerLedger = (params: Required<Pick<FundQuery, 'wmsTenantId' | 'currency' | 'startDate' | 'endDate'>>) => httpClient.get(`${OWNER}/ledger/export`, { params, responseType: 'blob' })
export const createOwnerRecharge = (data: RechargeCreateForm) => httpClient.post<ApiResult<RechargeOrder>>(`${OWNER}/recharges`, data)
export const cancelOwnerRecharge = (id: number) => httpClient.post<ApiResult<void>>(`${OWNER}/recharges/${id}/cancel`)

export const getOperatorOwnerAccounts = (params?: FundQuery) => httpClient.get<ApiResult<FundAccount[]>>(`${OPERATOR}/owner/accounts`, { params })
export const getOperatorOwnerRecharges = (params?: FundQuery) => httpClient.get<ApiResult<RechargeOrder[]>>(`${OPERATOR}/owner/recharges`, { params })
export const getOperatorOwnerLedger = (erpTenantId: number, params?: FundQuery) => httpClient.get<ApiResult<FundLedger[]>>(`${OPERATOR}/owner/ledger`, { params: { ...params, erpTenantId } })
export const reviewOwnerRecharge = (id: number, approved: boolean, reason?: string) => httpClient.post<ApiResult<void>>(`${OPERATOR}/owner/recharges/${id}/review`, { approved, reason })
export const reverseOwnerRecharge = (id: number, reason: string) => httpClient.post<ApiResult<void>>(`${OPERATOR}/owner/recharges/${id}/reverse`, { reason })

export const getOperatorPlatformAccounts = () => httpClient.get<ApiResult<FundAccount[]>>(`${OPERATOR}/platform/accounts`)
export const getOperatorPlatformRecharges = (params?: FundQuery) => httpClient.get<ApiResult<RechargeOrder[]>>(`${OPERATOR}/platform/recharges`, { params })
export const getOperatorPlatformLedger = (params?: FundQuery) => httpClient.get<ApiResult<FundLedger[]>>(`${OPERATOR}/platform/ledger`, { params })
export const createPlatformRecharge = (data: RechargeCreateForm) => httpClient.post<ApiResult<RechargeOrder>>(`${OPERATOR}/platform/recharges`, data)
export const cancelPlatformRecharge = (id: number) => httpClient.post<ApiResult<void>>(`${OPERATOR}/platform/recharges/${id}/cancel`)

export const getPlatformFundAccounts = (params?: FundQuery) => httpClient.get<ApiResult<FundAccount[]>>(`${PLATFORM}/accounts`, { params })
export const getPlatformRecharges = (params?: FundQuery) => httpClient.get<ApiResult<RechargeOrder[]>>(`${PLATFORM}/recharges`, { params })
export const getPlatformLedger = (wmsTenantId: number, params?: FundQuery) => httpClient.get<ApiResult<FundLedger[]>>(`${PLATFORM}/ledger`, { params: { ...params, wmsTenantId } })
export const getPlatformLedgerMonths = (wmsTenantId: number, params?: FundQuery) => httpClient.get<ApiResult<FundLedgerMonth[]>>(`${PLATFORM}/ledger/months`, { params: { ...params, wmsTenantId } })
export const exportPlatformLedger = (params: Required<Pick<FundQuery, 'wmsTenantId' | 'currency' | 'startDate' | 'endDate'>>) => httpClient.get(`${PLATFORM}/ledger/export`, { params, responseType: 'blob' })
export const reviewPlatformRecharge = (id: number, approved: boolean, reason?: string) => httpClient.post<ApiResult<void>>(`${PLATFORM}/recharges/${id}/review`, { approved, reason })
export const reversePlatformRecharge = (id: number, reason: string) => httpClient.post<ApiResult<void>>(`${PLATFORM}/recharges/${id}/reverse`, { reason })
