import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  ReturnDispositionDTO,
  ReturnInboundDetailVO,
  ReturnInboundPageParam,
  ReturnInboundPageVO
} from './types'

export function pageReturnInbound(pageParams: ReturnInboundPageParam) {
  return httpClient.get<ApiResult<PageResult<ReturnInboundPageVO>>>('/wms/return-inbound/page', {
    params: pageParams
  })
}

export function getReturnInboundDetail(id: number) {
  return httpClient.get<ApiResult<ReturnInboundDetailVO>>('/wms/return-inbound/detail', {
    params: { id }
  })
}

export function submitReturnDisposition(dto: ReturnDispositionDTO) {
  return httpClient.post<ApiResult<void>>('/wms/return-inbound/disposition', dto)
}

export function exportReturnInbound(qo: ReturnInboundPageParam) {
  return httpClient.get('/wms/return-inbound/export', { params: qo, responseType: 'blob' })
}
