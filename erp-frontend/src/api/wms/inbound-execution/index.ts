import httpClient from '@/utils/axios'
import type { ApiResult, PageResult } from '@/api/types'
import type {
  PurchaseInboundPageParam,
  PurchaseInboundPageVO,
  PurchaseInboundDetailVO
} from '@/api/wms/purchase-inbound/types'

/** 收货明细项 */
export interface ReceiveItem {
  skuCode: string
  actualQuantity: number
}

/** 收货 DTO */
export interface InboundReceiveDTO {
  inboundOrderId: number
  items: ReceiveItem[]
}

/** 上架分配行 */
export interface PutawayLine {
  skuCode: string
  locationCode: string
  quantity: number
  quality?: string
  zoneId?: number
}

/** 上架 DTO */
export interface InboundPutawayDTO {
  inboundOrderId: number
  lines: PutawayLine[]
}

/** 可用库位（上架分配用，空闲且分区匹配品质） */
export interface AvailableLocationVO {
  locationId: number
  locationCode: string
  zoneId: number
  zoneName?: string
  zoneType?: string
  rackNo?: string
  columnNo?: number
}

/** 平台待作业入库单分页（采购 + 自定义全来源，平台看全部） */
export function pageInboundOps(pageParams: PurchaseInboundPageParam) {
  return httpClient.get<ApiResult<PageResult<PurchaseInboundPageVO>>>(
    '/wms/inbound-execution/page',
    { params: pageParams }
  )
}

/** 平台入库单详情（收货/上架弹窗用，平台自有权限） */
export function getInboundOpsDetail(id: number) {
  return httpClient.get<ApiResult<PurchaseInboundDetailVO>>('/wms/inbound-execution/detail', {
    params: { id }
  })
}

/** 平台收货（录实收数量） */
export function receiveInbound(dto: InboundReceiveDTO) {
  return httpClient.post<ApiResult<void>>('/wms/inbound-execution/receive', dto)
}

/** 平台上架（分配库位写批次） */
export function putawayInbound(dto: InboundPutawayDTO) {
  return httpClient.post<ApiResult<void>>('/wms/inbound-execution/putaway', dto)
}

/** 上架可选库位（本货主服务商租用货架上、空闲 + 分区匹配品质；quality: GOOD/DAMAGED） */
export function listPutawayLocations(inboundOrderId: number, quality: string) {
  return httpClient.get<ApiResult<AvailableLocationVO[]>>(
    '/wms/inbound-execution/available-locations',
    { params: { inboundOrderId, quality } }
  )
}
