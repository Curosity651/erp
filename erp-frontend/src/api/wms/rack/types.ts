/** 货架（排）预览 */
export interface RackVO {
  warehouseId: number
  rackNo: string
  status: 'IDLE' | 'RESERVED' | 'OCCUPIED'
  assignmentId?: number
  assignedWmsTenantId?: number
  assignedWmsTenantName?: string
  contractControlled?: boolean
  contractId?: number
  contractNo?: string
  monthlyFee?: number
  effectiveFrom?: string
  effectiveTo?: string
  remark?: string
  expiringSoon?: boolean
  locationCount?: number
}

/** 货架分配请求 */
export interface RackAssignParam {
  wmsTenantId: number
  warehouseId: number
  rackNos: string[]
  monthlyFee: number
  effectiveFrom: string
  effectiveTo?: string
  contractFileUrl?: string
  remark?: string
}

/** WMS 服务商选项 */
export interface WmsOperatorOption {
  id: number
  name: string
}

/** 库位（下钻） */
export interface RackLocation {
  id: number
  warehouseId: number
  zoneId?: number
  rackNo: string
  columnNo?: number
  locationCode: string
  locationType?: string
  pickType?: string
}
