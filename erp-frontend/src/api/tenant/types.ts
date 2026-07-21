/**
 * 当前登录用户的身份（B 三层）
 */
export interface TenantIdentity {
  // 身份层级：OVERSEAS_PLATFORM=海外仓平台 / WMS_OPERATOR=WMS服务商 / ERP_USER=货主
  identityType: 'OVERSEAS_PLATFORM' | 'WMS_OPERATOR' | 'ERP_USER'
  // 实例/租户ID
  tenantId?: number
  // 租户类型
  tenantType?: string
  // 租户编码
  tenantCode?: string
  // 租户名称
  tenantName?: string
  // 是否本租户管理员
  admin?: boolean
}

/**
 * 开通租户请求（WMS 服务商 / 货主 共用）
 */
export interface OpenTenantParam {
  tenantCode: string
  tenantName: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  remark?: string
  adminUsername: string
  adminPassword: string
  adminNickname?: string
}

/**
 * 租户列表项
 */
export interface TenantBrief {
  id: number
  tenantCode: string
  tenantName: string
  tenantType: string
  parentWmsTenantId?: number
  status: number
  contactName?: string
  contactPhone?: string
  createTime?: string
}

/**
 * WMS 服务商分页查询参数
 */
export interface TenantPageParam {
  page?: number
  size?: number
  tenantCode?: string
  tenantName?: string
  status?: number
}
