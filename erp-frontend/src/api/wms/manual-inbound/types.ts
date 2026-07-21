import type { PageParam } from '@/api/types'

/**
 * 自定义入库单 DTO（不经采购/物流链路）
 */
export interface ManualInboundDTO {
  id?: number
  inboundNo: string
  warehouseId: number
  inboundDate: string
  remark?: string
  items: ManualInboundItemDTO[]
}

/**
 * 自定义入库单明细 DTO
 */
export interface ManualInboundItemDTO {
  skuCode: string
  expectedQuantity: number
  remark?: string
}

/**
 * 自定义入库单查询对象
 */
export interface ManualInboundQO {
  inboundNo?: string
  skuCode?: string
  warehouseId?: number
  orderStatus?: string
  inboundDateStart?: string
  inboundDateEnd?: string
}

/**
 * 自定义入库单分页参数
 */
export type ManualInboundPageParam = ManualInboundQO & PageParam

/**
 * 自定义入库单明细表单数据（用于表单页面）
 */
export interface ManualInboundItemFormData {
  skuCode: string
  expectedQuantity: number
  remark?: string
}
