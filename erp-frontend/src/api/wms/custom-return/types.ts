import type { PageParam } from '@/api/types'
import type {
  PurchaseInboundPageVO,
  PurchaseInboundDetailVO,
  PurchaseInboundItemVO
} from '@/api/wms/purchase-inbound/types'

/**
 * 自定义退货单 DTO（不挂靠平台订单的退货入库）
 */
export interface CustomReturnDTO {
  id?: number
  inboundNo: string
  returnType: string
  refNo?: string
  warehouseId: number
  inboundDate: string
  remark?: string
  items: CustomReturnItemDTO[]
}

/**
 * 自定义退货单明细 DTO
 */
export interface CustomReturnItemDTO {
  skuCode: string
  expectedQuantity: number
  expectedQuality?: string
  remark?: string
}

/**
 * 自定义退货单查询对象
 */
export interface CustomReturnQO {
  inboundNo?: string
  skuCode?: string
  warehouseId?: number
  returnType?: string
  orderStatus?: string
  inboundDateStart?: string
  inboundDateEnd?: string
}

/**
 * 自定义退货单分页参数
 */
export type CustomReturnPageParam = CustomReturnQO & PageParam

/**
 * 自定义退货单分页 VO（复用入库单 VO，追加退货字段）
 */
export interface CustomReturnPageVO extends PurchaseInboundPageVO {
  returnType?: string
  refNo?: string
}

/**
 * 自定义退货单明细 VO
 */
export interface CustomReturnItemVO extends PurchaseInboundItemVO {
  expectedQuality?: string
}

/**
 * 自定义退货单详情 VO
 */
export interface CustomReturnDetailVO extends PurchaseInboundDetailVO {
  returnType?: string
  refNo?: string
  items: CustomReturnItemVO[]
}

/**
 * 退货类型描述映射
 */
export const CUSTOM_RETURN_TYPE_MAP: Record<string, string> = {
  PLATFORM_BATCH: '平台批量退货',
  NO_ORDER: '无单退件',
  SAMPLE_BACK: '样品收回',
  WRONG_SHIPMENT: '发错召回',
  OTHER: '其他'
}

/**
 * 退货类型颜色映射（用于 Tag 组件）
 */
export const CUSTOM_RETURN_TYPE_COLOR_MAP: Record<string, string> = {
  PLATFORM_BATCH: 'blue',
  NO_ORDER: 'orange',
  SAMPLE_BACK: 'purple',
  WRONG_SHIPMENT: 'red',
  OTHER: 'default'
}

/**
 * 货品预判描述映射
 */
export const EXPECTED_QUALITY_MAP: Record<string, string> = {
  GOOD: '良品',
  PENDING: '待检',
  DAMAGED: '不良品'
}

/**
 * 货品预判颜色映射（用于 Tag 组件）
 */
export const EXPECTED_QUALITY_COLOR_MAP: Record<string, string> = {
  GOOD: 'success',
  PENDING: 'warning',
  DAMAGED: 'error'
}

/**
 * 自定义退货单明细表单数据（用于表单页面）
 */
export interface CustomReturnItemFormData {
  skuCode: string
  expectedQuantity: number
  expectedQuality?: string
  remark?: string
}
