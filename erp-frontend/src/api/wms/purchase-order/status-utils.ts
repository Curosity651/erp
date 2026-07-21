import type { PurchaseOrderStatus, PurchaseShippingStatus, PurchaseReceivingStatus } from './types'

/**
 * 采购单业务状态颜色映射
 */
export const ORDER_STATUS_COLOR_MAP: Record<PurchaseOrderStatus, string> = {
  DRAFT: 'default',
  CONFIRMED: 'blue',
  IN_PRODUCTION: 'processing',
  COMPLETED: 'success',
  CANCELLED: 'error'
}

/**
 * 发货状态颜色配置
 */
export const SHIPPING_STATUS_CONFIG: Record<
  PurchaseShippingStatus,
  { color: string; text: string }
> = {
  NOT_SHIPPED: { color: 'default', text: '未发货' },
  PARTIAL_SHIPPED: { color: 'gold', text: '部分发货' },
  ALL_SHIPPED: { color: 'cyan', text: '全部发货' }
}

/**
 * 入库状态颜色配置
 */
export const RECEIVING_STATUS_CONFIG: Record<
  PurchaseReceivingStatus,
  { color: string; text: string }
> = {
  NOT_RECEIVED: { color: 'default', text: '未入库' },
  PARTIAL_RECEIVED: { color: 'orange', text: '部分入库' },
  ALL_RECEIVED: { color: 'green', text: '全部入库' }
}

/**
 * 是否显示进度标签（仅生产中状态显示）
 */
export function shouldShowProgressTags(orderStatus: PurchaseOrderStatus): boolean {
  return orderStatus === 'IN_PRODUCTION'
}

/**
 * 计算进度百分比
 */
export function calculatePercent(current: number, total: number): number {
  if (total <= 0) return 0
  return Math.round((current / total) * 100)
}

/**
 * 可编辑字段配置
 */
export interface EditableFieldConfig {
  // 基本信息（采购单号、供应商、下单日期、预计交货日期、币种、含税）
  basicInfo: boolean
  // 采购明细
  items: boolean
  // 首付款比例、尾款账期
  paymentTerms: boolean
  // 付款状态和凭证
  paymentStatus: boolean
  // 合同附件
  contract: boolean
  // 质检数据
  qcData: boolean
  // 其他附件
  otherFiles: boolean
  // 备注
  remark: boolean
}

/**
 * 根据采购单状态获取可编辑字段配置
 * @param status 采购单状态
 */
export function getEditableFieldConfig(status: PurchaseOrderStatus): EditableFieldConfig {
  switch (status) {
    case 'DRAFT':
      // 草稿状态：所有字段可编辑
      return {
        basicInfo: true,
        items: true,
        paymentTerms: true,
        paymentStatus: true,
        contract: true,
        qcData: true,
        otherFiles: true,
        remark: true
      }
    case 'CONFIRMED':
    case 'IN_PRODUCTION':
      // 已确认至生产中：基本信息和明细不可编辑，其他可编辑
      return {
        basicInfo: false,
        items: false,
        paymentTerms: false,
        paymentStatus: true,
        contract: true,
        qcData: true,
        otherFiles: true,
        remark: true
      }
    case 'COMPLETED':
      // 已完成：仅付款凭证可编辑
      return {
        basicInfo: false,
        items: false,
        paymentTerms: false,
        paymentStatus: true,
        contract: false,
        qcData: false,
        otherFiles: false,
        remark: false
      }
    case 'CANCELLED':
      // 已取消：不可编辑
      return {
        basicInfo: false,
        items: false,
        paymentTerms: false,
        paymentStatus: false,
        contract: false,
        qcData: false,
        otherFiles: false,
        remark: false
      }
    default:
      return {
        basicInfo: false,
        items: false,
        paymentTerms: false,
        paymentStatus: false,
        contract: false,
        qcData: false,
        otherFiles: false,
        remark: false
      }
  }
}

/**
 * 列表页操作按钮配置
 */
export interface ListPageActions {
  // 编辑按钮
  edit: boolean
  // 确认按钮
  confirm: boolean
  // 开始生产按钮
  startProduction: boolean
  // 取消按钮
  cancel: boolean
  // 删除按钮
  delete: boolean
}

/**
 * 根据采购单状态获取列表页操作按钮配置
 * @param status 采购单状态
 */
export function getListPageActions(status: PurchaseOrderStatus): ListPageActions {
  switch (status) {
    case 'DRAFT':
      return {
        edit: true,
        confirm: true,
        startProduction: false,
        cancel: true,
        delete: true
      }
    case 'CONFIRMED':
      return {
        edit: true,
        confirm: false,
        startProduction: true,
        cancel: true,
        delete: false
      }
    case 'IN_PRODUCTION':
      return {
        edit: true,
        confirm: false,
        startProduction: false,
        cancel: false,
        delete: false
      }
    case 'COMPLETED':
      return {
        edit: true,
        confirm: false,
        startProduction: false,
        cancel: false,
        delete: false
      }
    case 'CANCELLED':
      return {
        edit: false,
        confirm: false,
        startProduction: false,
        cancel: false,
        delete: false
      }
    default:
      return {
        edit: false,
        confirm: false,
        startProduction: false,
        cancel: false,
        delete: false
      }
  }
}

/**
 * 详情页操作按钮配置
 */
export interface DetailPageActions {
  // 编辑按钮
  edit: boolean
  // 确认按钮
  confirm: boolean
  // 开始生产按钮
  startProduction: boolean
  // 取消按钮
  cancel: boolean
}

/**
 * 根据采购单状态获取详情页操作按钮配置
 * @param status 采购单状态
 */
export function getDetailPageActions(status: PurchaseOrderStatus): DetailPageActions {
  switch (status) {
    case 'DRAFT':
      return {
        edit: true,
        confirm: true,
        startProduction: false,
        cancel: true
      }
    case 'CONFIRMED':
      return {
        edit: true,
        confirm: false,
        startProduction: true,
        cancel: true
      }
    case 'IN_PRODUCTION':
      return {
        edit: true,
        confirm: false,
        startProduction: false,
        cancel: false
      }
    case 'COMPLETED':
      return {
        edit: true,
        confirm: false,
        startProduction: false,
        cancel: false
      }
    case 'CANCELLED':
      return {
        edit: false,
        confirm: false,
        startProduction: false,
        cancel: false
      }
    default:
      return {
        edit: false,
        confirm: false,
        startProduction: false,
        cancel: false
      }
  }
}

/**
 * 判断是否可以进入编辑页
 * @param status 采购单状态
 */
export function canEnterEditPage(status: PurchaseOrderStatus): boolean {
  return status !== 'CANCELLED'
}

/**
 * 获取状态下可编辑字段的提示信息
 * @param status 采购单状态
 */
export function getEditableFieldsHint(status: PurchaseOrderStatus): string {
  switch (status) {
    case 'DRAFT':
      return '当前为草稿状态，所有字段均可编辑'
    case 'CONFIRMED':
    case 'IN_PRODUCTION':
      return '当前状态下，仅可编辑：付款信息、合同附件、质检数据、其他附件、备注'
    case 'COMPLETED':
      return '当前状态下，仅可编辑：付款凭证'
    case 'CANCELLED':
      return '当前状态下不可编辑'
    default:
      return ''
  }
}
