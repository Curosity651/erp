/**
 * 海外仓平台 · 海外仓作业 · 退货质检 类型契约（业务需求 1.4）。
 *
 * 退货 = 带质检的重新入库：RETURN_PENDING 待收货 → QC_PENDING 待质检 → (逐SKU判定+上架) → COMPLETED。
 * QC 通过 → 退货区/标准区(quality=GOOD, 可分配, 生成新批次 FIFO 重排)；
 * QC 失败 → 不良品区(quality=DAMAGED, 不可分配)；FAIL 且电子类强制拍照(BR-04)。
 * 对齐后端表 wms_inbound_order(source_type=RETURN) + wms_return_qc_item / wms_physical_inventory / wms_zone。
 * 目前前端以 mock 驱动（见 ./mock），后端需按本契约实现（接口清单见 ./index 注释）。
 */

/** 退货单状态 */
export type ReturnStatus =
  | 'RETURN_PENDING' // 待退货收货
  | 'QC_PENDING' // 待质检
  | 'COMPLETED' // 退货入库完成
  | 'CLOSED' // 未收到或拒收关闭

/** 质检结果 */
export type QcResult = 'PASS' | 'FAIL' | 'MIXED'

/** 回库分区 */
export type ReturnZone =
  | 'RETURN' // 退货区（GOOD 可分配）
  | 'STANDARD' // 标准区（GOOD 可分配）
  | 'DEFECTIVE' // 不良品区（DAMAGED 不可分配）

/** 退货单明细 */
export interface ReturnOrderItemVO {
  skuCode: string
  skuName?: string
  // 电子类（影响 QC_FAIL 是否强制拍照）
  electronic: boolean
  quantityPerPallet?: number
  // 应退数
  expectedQty: number
  // 实收数（收货后回填）
  receivedQty?: number
  qualifiedQty?: number
  damagedQty?: number
  qualifiedZone?: ReturnZone
  qualifiedLocationCode?: string
  qualifiedPalletId?: number
  qualifiedSlotId?: number
  qualifiedSlotCode?: string
  damagedLocationCode?: string
  damagedPalletId?: number
  damagedSlotId?: number
  damagedSlotCode?: string
  // 质检结果（质检后回填）
  qcResult?: QcResult
  zone?: ReturnZone
  quality?: 'GOOD' | 'DAMAGED'
  locationCode?: string
  qcRemark?: string
  // 质检照片 OSS 文件ID（sys_file.id），只读回显用
  qcPhotoFileIds?: number[]
}

/** 退货单（列表 + 详情） */
export interface ReturnOrderVO {
  id: number
  returnNo: string
  erpTenantId: number
  ownerName: string
  // 所属WMS服务商ID
  operatorId?: number
  // 所属WMS服务商名称
  operatorName?: string
  warehouseId: number
  warehouseName: string
  skuKinds: number
  totalQty: number
  status: ReturnStatus
  createTime: string
  // 详情才带
  items?: ReturnOrderItemVO[]
}

/** 列表查询条件 */
export interface ReturnQO {
  returnNo?: string
  status?: ReturnStatus
  erpTenantId?: number
  /** WMS服务商筛选 */
  wmsTenantId?: number
  /** 创建日期起始 YYYY-MM-DD */
  createTimeStart?: string
  /** 创建日期结束 YYYY-MM-DD */
  createTimeEnd?: string
}

/** 退货收货入参（RETURN_PENDING → QC_PENDING） */
export interface ReturnReceiveDTO {
  returnOrderId: number
  items: { skuCode: string; receivedQty: number }[]
}

/** 质检明细行（逐 SKU） */
export interface ReturnQcLineDTO {
  skuCode: string
  qualifiedQty: number
  damagedQty: number
  qualifiedZone?: ReturnZone
  qualifiedLocationCode?: string
  qualifiedSlotCode?: string
  qualifiedPalletId?: number
  qualifiedCapacityPercent?: number
  damagedLocationCode?: string
  damagedSlotCode?: string
  damagedPalletId?: number
  damagedCapacityPercent?: number
  qcRemark?: string
  photoFileIds?: number[]
}

/** 质检 + 上架入参（QC_PENDING → COMPLETED） */
export interface ReturnQcDTO {
  returnOrderId: number
  warehouseId: number
  lines: ReturnQcLineDTO[]
}
