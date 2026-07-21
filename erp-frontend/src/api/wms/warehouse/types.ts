import type { PageParam } from '@/api/types'

/**
 * 仓库数据传输对象
 */
export interface WarehouseDTO {
  // 主键ID (编辑时必填)
  id?: number
  // 仓库编码
  warehouseCode: string
  // 仓库名称
  warehouseName: string
  // 仓库类型: OWN / FBO
  warehouseType: string
  // 所属区域ID
  regionId?: number
  // 关联平台 (FBO仓必填)
  platform?: string
  // 平台仓库ID (FBO仓必填)
  platformWarehouseId?: string
  // 仓库地址
  address?: string
  // 联系人
  contactName?: string
  // 联系电话
  contactPhone?: string
  // 状态: 1-启用 / 0-停用
  status: number
  // 备注
  remark?: string
}

/**
 * 仓库查询对象
 */
export interface WarehouseQO {
  // 仓库编码
  warehouseCode?: string
  // 仓库名称
  warehouseName?: string
  // 仓库类型
  warehouseType?: string
  // 状态
  status?: number
  // 所属区域ID
  regionId?: number
}

/**
 * 仓库分页参数
 */
export type WarehousePageParam = WarehouseQO & PageParam

/**
 * 仓库分页视图对象
 */
export interface WarehousePageVO {
  // 主键ID
  id: number
  // 仓库编码
  warehouseCode: string
  // 仓库名称
  warehouseName: string
  // 仓库类型
  warehouseType: string
  // 所属区域ID
  regionId: number
  // 所属区域名称
  regionName: string
  // 关联平台
  platform?: string
  // 平台仓库ID
  platformWarehouseId?: string
  // 状态: 1-启用 / 0-停用
  status: number
  // 联系人
  contactName?: string
  // 联系电话
  contactPhone?: string
  // 详细地址
  address?: string
  // 备注
  remark?: string
  // 创建时间
  createTime: string
  // 更新时间
  updateTime: string
}

/**
 * 仓库下拉选项视图对象
 */
export interface WarehouseOptionVO {
  // 仓库ID
  id: number
  // 仓库编码
  warehouseCode: string
  // 仓库名称
  warehouseName: string
  // 仓库类型
  warehouseType: string
  // 所属区域ID
  regionId: number
}

// ==================== FBO 仓库相关类型 ====================

/**
 * 平台 FBO 仓库信息
 */
export interface FboWarehouseVO {
  // 平台仓库ID
  platformWarehouseId: string
  // 仓库名称
  warehouseName: string
  // 是否已存在于ERP
  exists: boolean
}

/**
 * FBO 仓库导入项
 */
export interface FboWarehouseImportItem {
  // 平台仓库ID
  platformWarehouseId: string
  // 仓库名称
  warehouseName: string
}

/**
 * FBO 仓库导入请求
 */
export interface FboWarehouseImportDTO {
  // 平台
  platform: string
  // 要导入的仓库列表
  items: FboWarehouseImportItem[]
}

/**
 * 导入的仓库信息
 */
export interface ImportedWarehouse {
  // 仓库编码
  warehouseCode: string
  // 仓库名称
  warehouseName: string
}

/**
 * FBO 仓库导入结果
 */
export interface FboWarehouseImportResultVO {
  // 成功导入数量
  successCount: number
  // 导入的仓库列表
  warehouses: ImportedWarehouse[]
}
