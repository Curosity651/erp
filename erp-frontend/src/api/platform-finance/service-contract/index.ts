import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

export interface ServiceContract {
  id: number
  contractNo: string
  wmsTenantId: number
  warehouseId: number
  startDate: string
  endDate: string
  rackUnitCount: number
  monthlyRentPerUnit: number
  warehouseDeposit: number
  subscriptionTotal: number
  refundableRate: number
  refundableAmount: number
  serviceAmount: number
  monthlyServiceRecognition: number
  contractFileUrl?: string
  contractStatus: 'DRAFT' | 'ACTIVE' | 'SETTLED' | 'TERMINATED'
  remark?: string
  createTime: string
}

export interface ContractFundLedger {
  id: number
  contractId: number
  fundComponent: string
  transactionType: string
  direction: 'IN' | 'OUT'
  amount: number
  currency: string
  accountingMonth?: string
  status: string
  remark?: string
  createTime: string
}

export interface ServiceContractCreate {
  contractNo: string
  wmsTenantId: number
  warehouseId: number
  startDate: string
  endDate: string
  rackUnitCount: number
  monthlyRentPerUnit: number
  warehouseDeposit: number
  subscriptionTotal: number
  refundableRate: number
  contractFileUrl?: string
  remark?: string
  rackNos?: string[]
}

export function listServiceContracts() {
  return httpClient.get<ApiResult<ServiceContract[]>>('/platform-finance/service-contracts')
}

export function createServiceContract(data: ServiceContractCreate) {
  return httpClient.post<ApiResult<ServiceContract>>('/platform-finance/service-contracts', data)
}

export function listContractFunds(contractId: number) {
  return httpClient.get<ApiResult<ContractFundLedger[]>>(
    '/platform-finance/service-contracts/funds',
    { params: { contractId } }
  )
}

export function recognizeContractService(contractId: number, month: string) {
  return httpClient.post<ApiResult<void>>('/platform-finance/service-contracts/recognize', null, {
    params: { contractId, month }
  })
}

export function refundServiceContract(data: {
  contractId: number
  noDefault: boolean
  noDebt: boolean
  noRemainingGoods: boolean
  remark?: string
}) {
  return httpClient.post<ApiResult<void>>('/platform-finance/service-contracts/refund', data)
}
