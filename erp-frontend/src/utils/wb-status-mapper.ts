import { WB_PLATFORM_STATUS_MAP, WB_SUPPLIER_STATUS_MAP } from '@/api/order/wb-order/types'
import { createStatusMapper } from '@/utils/order-status-mapper'

// 三平台共用的 ERP 状态映射 - 从 order-status-mapper re-export
export { mapErpStatus } from '@/utils/order-status-mapper'

/**
 * WB 平台履约状态映射（platformStatus）
 */
export const mapWbPlatformStatus = createStatusMapper(WB_PLATFORM_STATUS_MAP)

/**
 * WB 商家处理状态映射（platformSubstatus）
 */
export const mapWbSupplierStatus = createStatusMapper(WB_SUPPLIER_STATUS_MAP)
