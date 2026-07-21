import type { PageParam } from '@/api/types'

/**
 * 物流商数据传输对象
 */
export interface LogisticsProviderDTO {
  // 主键ID (编辑时必填)
  id?: number
  // 物流商编码
  providerCode: string
  // 物流商名称
  providerName: string
  // 联系人
  contactName?: string
  // 联系电话
  contactPhone?: string
  // 联系邮箱
  contactEmail?: string
  // 状态: 1-启用 / 0-停用
  status: number
  // 备注
  remark?: string
}

/**
 * 物流商查询对象
 */
export interface LogisticsProviderQO {
  // 物流商编码
  providerCode?: string
  // 物流商名称
  providerName?: string
  // 状态
  status?: number
}

/**
 * 物流商分页参数
 */
export type LogisticsProviderPageParam = LogisticsProviderQO & PageParam

/**
 * 物流商分页视图对象
 */
export interface LogisticsProviderPageVO {
  // 主键ID
  id: number
  // 物流商编码
  providerCode: string
  // 物流商名称
  providerName: string
  // 联系人
  contactName?: string
  // 联系电话
  contactPhone?: string
  // 联系邮箱
  contactEmail?: string
  // 状态: 1-启用 / 0-停用
  status: number
  // 备注
  remark?: string
  // 创建时间
  createTime: string
  // 更新时间
  updateTime: string
}

/**
 * 物流商下拉选项视图对象
 */
export interface LogisticsProviderOptionVO {
  // 物流商ID
  id: number
  // 物流商编码
  providerCode: string
  // 物流商名称
  providerName: string
}
