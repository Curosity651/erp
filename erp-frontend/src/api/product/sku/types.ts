import type { PageParam } from '@/api/types'

/**
 * 产品状态枚举
 */
export enum ProductStatus {
  ON_SALE = 1, // 在售
  OFF_SALE = 2, // 停售
  DEVELOPING = 3, // 开发中
  DISCONTINUED = 4 // 已下架
}

/**
 * 头程类型枚举
 */
export enum ShippingType {
  LAND = 1, // 陆运
  AIR = 2 // 空运
}

/**
 * SKU文件DTO - 用于提交文件信息
 */
export interface SkuFileDTO {
  /**
   * 文件类型
   * actual_image、platform_image、manual、box_mark、install_video、quality_report、
   * platform_render_video、model_3d、engineering_drawing、bom、drop_test_video、
   * package_manual、quotation、sample_confirmation
   */
  fileType: string
  /**
   * OSS对象键 - 用于构建文件访问URL
   */
  objectKey: string
}

/**
 * SKU文件VO - 用于展示文件信息
 */
export interface SkuFileVO {
  /**
   * 主键ID
   */
  id: number
  /**
   * 文件类型
   * actual_image、platform_image、manual、box_mark等
   */
  fileType: string
  /**
   * OSS对象键 - 用于构建文件访问URL
   */
  objectKey: string
  /**
   * 文件访问URL - 前端通过objectKey构建的完整URL
   */
  fileUrl: string
  /**
   * 排序顺序 - 数值越小越靠前
   */
  sortOrder?: number
}

/**
 * SKU基础字段定义 - 用于共享字段定义
 */
interface SkuBaseFields {
  // SKU编码
  skuCode: string
  // 商品条码，一个ERP SKU可绑定多个EAN/UPC或内部条码
  barcodes?: string[]
  // SKU序号，唯一
  skuNo?: number
  // SPU编码
  spuCode: string
  // 销售国家
  salesCountry?: string
  // 品类ID
  categoryId?: number
  // 产品状态 1:在售 2:停售 3:开发中 4:已下架
  productStatus?: number
  // 品牌编码
  brandCode?: string
  // 项目组编码
  projectGroupCode?: string
  // 中文名
  chineseName?: string
  // 俄文名
  russianName?: string
  // 产品描述
  description?: string
  // 备注
  remarks?: string
  // 海关申报名
  customsDeclarationName?: string
  // 海关申报代码
  customsDeclarationCode?: string
  // 是否有排插 1=是，0=否
  needsPower?: number
  // 季节性产品，1=是，0=否
  seasonal?: number
  // 是否有RGB灯带，1=是，0=否
  hasRgbLight?: number
  // 是否有玻璃，1=是，0=否
  hasGlass?: number
  // 头程 1:陆运 2:空运
  shippingType?: number
  // 包裹类型, normal=普通，magnetic=含磁
  packageType?: string
  // 计费重类型, 1=取大值，2=实重，3=体积重
  billingWeightType?: number
  // BC纸箱最低耐破
  bcBoxMinBreakage?: string
  // 分箱
  packaging?: string
  // 表面颜色
  surfaceColor?: string
  // 钢架颜色
  frameColor?: string
  // 材质
  material?: string
  // 重量
  weight?: number
  // 重量单位
  weightUnit?: string
  // 每托数量（件）
  quantityPerPallet?: number
  // 包装长度
  packageLength?: number
  // 包装宽度
  packageWidth?: number
  // 包装高度
  packageHeight?: number
  // 包装尺寸单位
  packageUnit?: string
  // 外箱长度（毫米，接口存储单位）
  outerLengthMm?: number
  // 外箱宽度（毫米，接口存储单位）
  outerWidthMm?: number
  // 外箱高度（毫米，接口存储单位）
  outerHeightMm?: number
  // 单箱毛重（克，接口存储单位）
  outerGrossWeightG?: number
  // 功能性能要求
  functionalRequirements?: string
  // 供应商编码
  supplierCode?: string
  // 是否含税 1=含税，0=不含税
  includeTax?: number
  // 税率
  taxRate?: number
  // 采购价 - 数字类型（与后端BigDecimal对应）
  purchasePrice?: number
  // 起订量
  minimumOrderQuantity?: number
  // 生产周期(天)
  productionCycle?: number
  // 开发人员ID
  developerId?: number
  // 运营人员ID
  operatorId?: number
  // 质检人员ID
  qcId?: number
  // 采购人员ID
  purchaserId?: number
}

/**
 * SKU数据传输对象 - 用于内部数据传递，不直接用于API
 */
export interface SkuDTO extends SkuBaseFields {
  // 文件信息 - 按文件类型分组的文件列表
  files?: Record<string, SkuFileDTO[]>
}

/**
 * SKU创建DTO - 用于创建SKU的输入数据，与后端SkuCreateDTO保持一致
 */
export type SkuCreateDTO = SkuDTO

/**
 * SKU更新DTO - 用于更新SKU的输入数据，与后端SkuUpdateDTO保持一致
 */
export interface SkuUpdateDTO extends SkuDTO {
  // 主键ID - 更新时必需
  id: number
}

export interface SkuQO {
  // SKU编码
  skuCode?: string
  // SKU序号
  skuNo?: number
  // SPU编码
  spuCode?: string
  // 销售国家
  salesCountry?: string
  // 品类ID
  categoryId?: number
  // 产品状态 1:在售 2:停售 3:开发中 4:已下架
  productStatus?: number
  // 头程 1:陆运 2:空运
  shippingType?: number
  // 品牌编码
  brandCode?: string
  // 项目组编码
  projectGroupCode?: string
  // 中文名
  chineseName?: string
  // 俄文名
  russianName?: string
  // 供应商编码
  supplierCode?: string
  // 创建时间开始
  createTimeStart?: string
  // 创建时间结束
  createTimeEnd?: string
  // 包裹类型
  packageType?: string
  // 表面颜色
  surfaceColor?: string
  // 钢架颜色
  frameColor?: string
  // 材质
  material?: string
  // 是否有排插 0:否 1:是
  needsPower?: number
  // 是否季节性产品 0:否 1:是
  seasonal?: number
  // 是否有RGB灯带 0:否 1:是
  hasRgbLight?: number
  // 是否有玻璃 0:否 1:是
  hasGlass?: number
  // 计费重类型 1:取大值 2:实重 3:体积重
  billingWeightType?: number
  // 开发人员ID
  developerId?: number
  // 运营人员ID
  operatorId?: number
  // 质检人员ID
  qualityInspectorId?: number
  // 采购人员ID
  purchaserId?: number
}

/**
 * SKU管理表分页参数
 */
export type SkuPageParam = SkuQO & PageParam

/**
 * SKU基础视图对象 - 包含展示时需要的扩展字段
 */
interface SkuBaseVO extends SkuBaseFields {
  // 主键ID
  id: number
  // 品类名称
  categoryName?: string
  // 品牌名称
  brandName?: string
  // 项目组名称
  projectGroupName?: string
  // 开发人员姓名
  developerName?: string
  // 运营人员姓名
  operatorName?: string
  // 质检人员姓名
  qcName?: string
  // 采购人员姓名
  purchaserName?: string
  // 包装体积（计算字段）
  packageVolume?: string
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
  // 创建人
  createBy?: string
  // 更新人
  updateBy?: string
  // 文件信息 - 按文件类型分组的文件列表
  files?: Record<string, SkuFileVO[]>
}

/**
 * 品类路径节点
 */
export interface CategoryPathNode {
  // 品类ID
  id: number
  // 品类名称
  name: string
  // 品类编码
  code: string
  // 品类层级
  level: number
}

/**
 * 品类层级视图对象
 */
export interface CategoryHierarchyVO {
  // 当前品类ID
  categoryId: number
  // 当前品类名称
  categoryName: string
  // 当前品类编码
  categoryCode: string
  // 当前品类层级
  categoryLevel: number
  // 父级品类路径（从根到当前品类的完整路径）
  parentPath: CategoryPathNode[]
  // 完整路径名称（用/分隔）
  fullPathName: string
}

/**
 * SKU管理表分页视图对象 - 与后端SkuPageVO保持一致
 */
export interface SkuPageVO extends SkuBaseVO {
  // 品类名称 - 分页列表中必需
  categoryName: string
  // 品类层级信息
  categoryHierarchy?: CategoryHierarchyVO
  // 品牌名称 - 分页列表中必需
  brandName: string
  // 项目组名称 - 分页列表中必需
  projectGroupName: string
  // 平台映射数量
  mappingCount?: number
}

/**
 * SKU详情视图对象 - 用于展示SKU详细信息，与后端SkuDetailVO保持一致
 */
export type SkuDetailVO = SkuBaseVO

/**
 * SKU编码验证结果
 */
export interface SkuCodeValidationResult {
  // 是否唯一
  isUnique: boolean
  // 验证消息
  message?: string
}

/**
 * SKU统计数据
 */
export interface SkuStatsData {
  // 统计项名称
  name: string
  // 统计值
  value: number
  // 百分比
  percentage?: number
}

/**
 * SKU概览统计
 */
export interface SkuOverviewStats {
  // 总数量
  totalCount: number
  // 在售数量
  onSaleCount: number
  // 停售数量
  offSaleCount: number
  // 开发中数量
  developingCount: number
  // 已下架数量
  discontinuedCount: number
  // 各品牌统计
  brandStats: SkuStatsData[]
  // 各供应商统计
  supplierStats: SkuStatsData[]
  // 各状态统计
  statusStats: SkuStatsData[]
}

/**
 * SKU筛选选项
 */
export interface SkuFilterOptions {
  // 品牌列表
  brands: string[]
  // 项目组列表
  projectGroups: string[]
  // 供应商编码列表
  supplierCodes: string[]
  // 销售国家列表
  salesCountries: string[]
  // 产品状态选项
  productStatuses: Array<{
    label: string
    value: number
  }>
  // 头程类型选项
  shippingTypes: Array<{
    label: string
    value: number
  }>
}

/**
 * SKU搜索参数
 */
export interface SkuSearchParam {
  // 关键字
  keyword?: string
  // 搜索文本（智能搜索）
  searchText?: string
  // 品类ID列表
  categoryIds?: number[]
  // 产品状态列表
  productStatuses?: number[]
  // 品牌列表
  brands?: string[]
  // 供应商编码列表
  supplierCodes?: string[]
  // 项目组列表
  projectGroups?: string[]
  // 销售国家列表
  salesCountries?: string[]
  // 头程类型列表
  shippingTypes?: number[]
  // 创建时间范围
  createTimeRange?: [string, string]
  // 更新时间范围
  updateTimeRange?: [string, string]
}

/**
 * 批量操作参数
 */
export interface SkuBatchOperationParam {
  // SKU ID列表
  skuIds: number[]
  // 操作类型
  operationType: 'updateStatus' | 'delete' | 'export'
  // 操作参数
  operationParams?: Record<string, any>
}

// ==================== SKU选择弹窗专用类型 ====================

/**
 * SKU选择弹窗查询对象
 */
export interface SkuSelectQO {
  /** SKU编码（模糊查询） */
  skuCode?: string
  /** SKU名称（中文名或俄文名，模糊查询） */
  skuName?: string
  /** 所属货主ID（平台端跨货主选品用；非平台身份服务端忽略） */
  erpTenantId?: number
}

/**
 * SKU选择弹窗视图对象（轻量级）
 */
export interface SkuSelectVO {
  /** 主键ID */
  id: number
  /** SKU编码 */
  skuCode: string
  /** 中文名 */
  chineseName?: string
  /** 主图URL */
  mainImage?: string
  /** 品类全路径名称 */
  categoryFullPath?: string
}
