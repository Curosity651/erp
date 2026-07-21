/**
 * 面单批次 API 和类型定义
 * 用于 Wildberries 和 Ozon 平台的面单打印功能
 */
import httpClient from '@/utils/axios'
import type { ApiResult, PageParam, PageResult } from '@/api/types'

// ==================== 类型定义 ====================

/**
 * 面单批次分页列表 VO
 * 对应后端：LabelBatchPageVO.java
 * 用于分页查询，不包含文件列表和失败订单项详情
 */
export interface LabelBatchPageVO {
  /** 批次 ID */
  batchId: number
  /** 批次号 */
  batchNo: string
  /** 平台 (Wildberries | Ozon) */
  platform: string
  /** 订单总数 */
  totalOrders: number
  /** 文件总数 */
  totalFiles: number
  /** 成功订单数 */
  successCount: number
  /** 失败订单数 */
  failedCount: number
  /** 批次状态：GENERATED | PARTIAL | FAILED */
  status: string
  /** 备注 */
  remark?: string
  /** 创建人 ID */
  createdBy: number
  /** 创建时间 */
  createTime: string
}

/**
 * 面单批次详情 VO
 * 对应后端：LabelBatchVO.java
 * 包含完整的文件列表和失败订单项
 */
export interface LabelBatchVO {
  /** 批次 ID */
  batchId: number
  /** 批次号 */
  batchNo: string
  /** 平台 (Wildberries | Ozon) */
  platform: string
  /** 订单总数 */
  totalOrders: number
  /** 文件总数 */
  totalFiles: number
  /** 成功订单数 */
  successCount: number
  /** 失败订单数 */
  failedCount: number
  /** 批次状态：GENERATED | PARTIAL | FAILED */
  status: string
  /** 备注 */
  remark?: string
  /** 创建人 ID */
  createdBy: number
  /** 创建时间 */
  createTime: string
  /** 文件列表 */
  files: LabelBatchFileVO[]
  /** 失败的订单项列表 */
  failedItems: LabelBatchItemVO[]
}

/**
 * 面单批次文件 VO
 * 对应后端：LabelBatchFileVO.java
 */
export interface LabelBatchFileVO {
  /** 文件 ID */
  fileId: number
  /** 文件类型 */
  type: string
  /** 目标仓库 ID（用于分组） */
  destinationWarehouseId?: string
  /** 目标仓库名称 */
  destinationWarehouseName?: string
  /** ERP SKU 编码 */
  erpSkuCode?: string
  /** ERP SKU 编号 */
  erpSkuNo?: string
  /** SKU 数量 */
  skuCount?: number
  /** 文件名 */
  fileName?: string
  /** 对象存储 key */
  objectKey?: string
  /** 页数 */
  pageCount?: number
  /** 下载链接 */
  downloadUrl?: string
}

/**
 * 面单批次订单项 VO
 * 对应后端：LabelBatchItemVO.java
 */
export interface LabelBatchItemVO {
  /** 订单项 ID */
  itemId: number
  /** 订单 ID */
  orderId: number
  /** 平台订单号 */
  platformOrderId?: string
  /** 平台 */
  platform?: string
  /** ERP SKU 编码 */
  erpSkuCode?: string
  /** SKU 数量 */
  skuQty?: number
  /** 订单项状态: PENDING | SUCCESS | FAILED */
  status: string
  /** 失败码 */
  failCode?: string
  /** 失败原因 */
  errorMsg?: string
  /** 仓库 ID */
  warehouseId?: string
  /** 供应商 ID */
  supplyId?: string
  /** 店铺 ID */
  shopId?: number
}

/**
 * 面单批次查询对象
 */
export interface LabelBatchQO {
  /** 平台过滤 */
  platform?: string
  /** 批次号 */
  batchNo?: string
  /** 状态 */
  status?: string
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
}

// ==================== API 函数 ====================

/**
 * 分页查询面单批次列表
 * @param pageParam 分页参数
 * @param qo 查询条件
 * @returns 分页结果（不包含文件列表和失败订单项）
 */
export function pageBatches(pageParam: PageParam, qo: LabelBatchQO) {
  return httpClient.get<ApiResult<PageResult<LabelBatchPageVO>>>('/order/label-batch/page', {
    params: { ...pageParam, ...qo }
  })
}

/**
 * 获取批次详情
 * @param batchId 批次ID
 */
export function getBatchDetail(batchId: number) {
  return httpClient.get<ApiResult<LabelBatchVO>>(`/order/label-batch/${batchId}`)
}
