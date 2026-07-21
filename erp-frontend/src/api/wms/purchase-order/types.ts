import type { PageParam } from '@/api/types'
import type { SkuBriefVO } from '@/api/common/sku-types'

// ==================== 枚举类型 ====================

/**
 * 采购单业务状态（用户操作驱动）
 */
export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'CONFIRMED'
  | 'IN_PRODUCTION'
  | 'COMPLETED'
  | 'CANCELLED'

/**
 * 发货状态（物流单触发）
 */
export type PurchaseShippingStatus = 'NOT_SHIPPED' | 'PARTIAL_SHIPPED' | 'ALL_SHIPPED'

/**
 * 入库状态（入库单触发）
 */
export type PurchaseReceivingStatus = 'NOT_RECEIVED' | 'PARTIAL_RECEIVED' | 'ALL_RECEIVED'

/**
 * 采购单附件类型
 */
export type PurchaseOrderFileType =
  | 'CONTRACT'
  | 'QUALITY_REPORT'
  | 'PREPAY_VOUCHER'
  | 'BALANCE_VOUCHER'
  | 'OTHER'

/**
 * 付款类型
 */
export type PaymentType = 'PREPAY' | 'BALANCE'

/**
 * 合同操作类型
 */
export type ContractAction = 'UPLOAD' | 'REPLACE'

// ==================== 新增DTO类型 ====================

/**
 * 付款信息DTO
 */
export interface PaymentInfoDTO {
  // 首付款状态: 0-未付 / 1-已付
  prepayStatus?: number
  // 首付款凭证文件ID（状态为已付时必填）
  prepayVoucherFileId?: number
  // 尾款状态: 0-未付 / 1-已付
  balanceStatus?: number
  // 尾款凭证文件ID（状态为已付时必填）
  balanceVoucherFileId?: number
}

/**
 * 合同信息DTO
 */
export interface ContractInfoDTO {
  // 合同文件ID（关联sys_file表）
  contractFileId?: number
  // 操作类型: UPLOAD-上传 / REPLACE-替换 / null-不变
  action?: ContractAction
}

/**
 * 质检数据项DTO
 */
export interface QcItemDTO {
  // SKU编码
  skuCode: string
  // 包装长度(cm)
  lengthCm?: number
  // 包装宽度(cm)
  widthCm?: number
  // 包装高度(cm)
  heightCm?: number
  // 毛重(KG)
  grossWeightKg?: number
  // 净重(KG)
  netWeightKg?: number
  // 质检报告文件ID
  qcFileId?: number
}

/**
 * 质检数据DTO
 */
export interface QcDataDTO {
  // 各SKU的质检数据列表
  items?: QcItemDTO[]
}

/**
 * 文件信息VO
 */
export interface FileInfoVO {
  // 采购单附件关联ID
  id: number
  // 系统文件ID
  sysFileId: number
  // 原始文件名
  fileName: string
  // 文件大小(字节)
  fileSize: number
  // MIME类型
  contentType: string
  // 文件类型
  fileType: string
  // 文件类型描述
  fileTypeDesc: string
  // 上传时间
  createTime: string
}

// ==================== 采购单 ====================

/**
 * 采购单DTO
 */
export interface PurchaseOrderDTO {
  // 主键ID (编辑时必填)
  id?: number
  // 采购单号（合同编号）
  orderNo: string
  // 供应商ID
  supplierId: number
  // 下单日期
  orderDate: string
  // 预计交货日期
  expectedDeliveryDate?: string
  // 实际交货日期
  actualDeliveryDate?: string
  // 币种
  currencyCode?: string
  // 是否含税
  taxIncluded?: number
  // 首付款比例
  prepayRatio?: number
  // 尾款账期(天)
  balancePaymentDays?: number
  // 备注
  remark?: string
  // 采购明细
  items: PurchaseOrderItemDTO[]
  // 付款信息
  paymentInfo?: PaymentInfoDTO
  // 合同信息
  contractInfo?: ContractInfoDTO
  // 质检数据
  qcData?: QcDataDTO
  // 其他附件文件ID列表
  otherFileIds?: number[]
}

/**
 * 采购单明细DTO
 */
export interface PurchaseOrderItemDTO {
  // 主键ID
  id?: number
  // SKU编码
  skuCode: string
  // 采购数量
  quantity: number
  // 单价
  unitPrice: number
  // 备注
  remark?: string
}

/**
 * 采购单查询对象
 */
export interface PurchaseOrderQO {
  // 采购单号
  orderNo?: string
  // 供应商ID
  supplierId?: number
  // 业务状态
  orderStatus?: PurchaseOrderStatus
  // 发货状态
  shippingStatus?: PurchaseShippingStatus
  // 入库状态
  receivingStatus?: PurchaseReceivingStatus
  // 首付款状态
  prepayStatus?: number
  // 尾款状态
  balanceStatus?: number
  // 下单日期开始
  orderDateStart?: string
  // 下单日期结束
  orderDateEnd?: string
  // SKU编码
  skuCode?: string
}

/**
 * 采购单分页参数
 */
export type PurchaseOrderPageParam = PurchaseOrderQO & PageParam

/**
 * 采购单分页VO
 */
export interface PurchaseOrderPageVO {
  // 主键ID
  id: number
  // 采购单号
  orderNo: string
  // 供应商ID
  supplierId: number
  // 供应商编码
  supplierCode: string
  // 供应商名称
  supplierName: string
  // 下单日期
  orderDate: string
  // 预计交货日期
  expectedDeliveryDate?: string
  // 实际交货日期
  actualDeliveryDate?: string
  // 合同总金额
  totalAmount: number
  // 币种
  currencyCode: string
  // 首付款金额
  prepayAmount: number
  // 首付款状态
  prepayStatus: number
  // 尾款状态
  balanceStatus: number
  // 订单状态
  orderStatus: PurchaseOrderStatus
  // 订单状态描述
  orderStatusDesc: string
  // 发货状态
  shippingStatus: PurchaseShippingStatus
  // 发货状态描述
  shippingStatusDesc: string
  // 入库状态
  receivingStatus: PurchaseReceivingStatus
  // 入库状态描述
  receivingStatusDesc: string
  // SKU数量
  skuCount: number
  // 总采购数量
  totalQuantity: number
  // 总发货数量
  totalShippedQuantity: number
  // 总入库数量
  totalReceivedQuantity: number
  // 创建时间
  createTime: string
}

/**
 * 采购单详情VO
 */
export interface PurchaseOrderDetailVO {
  // 主键ID
  id: number
  // 采购单号
  orderNo: string
  // 供应商ID
  supplierId: number
  // 供应商名称
  supplierName?: string
  // 下单日期
  orderDate: string
  // 预计交货日期
  expectedDeliveryDate?: string
  // 实际交货日期
  actualDeliveryDate?: string
  // 合同总金额
  totalAmount: number
  // 币种
  currencyCode: string
  // 是否含税
  taxIncluded: number
  // 首付款比例
  prepayRatio: number
  // 首付款金额
  prepayAmount: number
  // 尾款账期(天)
  balancePaymentDays?: number
  // 首付款状态
  prepayStatus: number
  // 首付款时间
  prepayTime?: string
  // 尾款状态
  balanceStatus: number
  // 尾款时间
  balancePayTime?: string
  // 订单状态
  orderStatus: PurchaseOrderStatus
  // 订单状态描述
  orderStatusDesc: string
  // 发货状态
  shippingStatus: PurchaseShippingStatus
  // 发货状态描述
  shippingStatusDesc: string
  // 入库状态
  receivingStatus: PurchaseReceivingStatus
  // 入库状态描述
  receivingStatusDesc: string
  // 备注
  remark?: string
  // 采购明细
  items: PurchaseOrderItemVO[]
  // 总采购数量
  totalQuantity: number
  // 总发货数量
  totalShippedQuantity: number
  // 总入库数量
  totalReceivedQuantity: number
  // 创建时间
  createTime: string
  // 更新时间
  updateTime: string
  // 首付款凭证文件信息
  prepayVoucherFile?: FileInfoVO
  // 尾款凭证文件信息
  balanceVoucherFile?: FileInfoVO
  // 合同文件信息
  contractFile?: FileInfoVO
  // 质检数据列表
  qcItems?: PurchaseOrderQcItemVO[]
  // 其他附件列表
  otherFiles?: FileInfoVO[]
  // SKU展示信息映射表
  skuBriefMap?: Record<string, SkuBriefVO>
}

/**
 * 采购单明细VO
 */
export interface PurchaseOrderItemVO {
  // 主键ID
  id: number
  // 采购单ID
  purchaseOrderId: number
  // SKU编码
  skuCode: string
  // 采购数量
  quantity: number
  // 单价
  unitPrice: number
  // 金额
  amount: number
  // 已发货数量
  shippedQuantity: number
  // 已入库数量
  receivedQuantity: number
  // 备注
  remark?: string
}

/**
 * 采购单附件VO
 */
export interface PurchaseOrderFileVO {
  // 主键ID
  id: number
  // 采购单ID
  purchaseOrderId: number
  // 系统文件ID
  sysFileId: number
  // 文件类型
  fileType: PurchaseOrderFileType
  // 文件类型描述
  fileTypeDesc: string
  // 原始文件名
  fileName: string
  // 文件大小(字节)
  fileSize: number
  // MIME类型
  contentType: string
  // 备注
  remark?: string
  // 上传人
  createBy?: number
  // 创建时间
  createTime: string
}

// ==================== 质检数据 ====================

/**
 * 质检数据VO
 */
export interface PurchaseOrderQcItemVO {
  // 主键ID
  id: number
  // 采购单ID
  purchaseOrderId: number
  // SKU编码
  skuCode: string
  // 包装长度(cm)
  lengthCm: number
  // 包装宽度(cm)
  widthCm: number
  // 包装高度(cm)
  heightCm: number
  // 体积(cm³)
  volumeCm3: number
  // 毛重(KG)
  grossWeightKg: number
  // 净重(KG)
  netWeightKg: number
  // 质检报告文件ID
  qcFileId?: number
  // 质检报告文件名
  qcFileName?: string
  // 备注
  remark?: string
  // 创建人
  createBy?: number
  // 创建时间
  createTime: string
  // 更新人
  updateBy?: number
  // 更新时间
  updateTime: string
}
