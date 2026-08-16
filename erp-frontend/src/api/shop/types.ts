// Legacy types removed: ShopDTO / ShopPageVO and related query types.

// ===== Unified V2 Types (previously in index.ts) =====
export interface ShopListQuery {
  platform?: string
  status?: number
  keyword?: string
  page?: number
  size?: number
}

export interface ShopVO {
  id: number
  platform: string
  name: string
  erpShopName: string
  platformShopId: string
  status: number
  lastTestStatus?: number
  lastTestedAt?: string
}

export interface ShopDetailVO extends ShopVO {
  credentialMask?: Record<string, string>
  defaultWmsWarehouseId?: number
}

export interface TestCredentialRequest {
  platform: string
  credential: Record<string, string>
}

export interface TestCredentialResponse {
  platform: string
  shopName?: string
  platformShopId: string
  testToken: string
  shopNameRequired: boolean
}

export interface CreateOrUpdateShopRequest {
  platform: string
  credential: Record<string, string>
  shopName: string
  erpShopName: string
  platformShopId: string
  testToken?: string
  defaultWmsWarehouseId: number
}

// ===== Status Constants & Enums =====

export const ShopStatus = {
  DISABLED: 0,
  ENABLED: 1,
  ABNORMAL: 2
} as const
export type ShopStatusType = (typeof ShopStatus)[keyof typeof ShopStatus]

export const ShopTestStatus = {
  NEVER: 0, // 从未测试
  SUCCESS: 1, // 最近一次测试成功
  FAIL: 2 // 最近一次测试失败
} as const
export type ShopTestStatusType = (typeof ShopTestStatus)[keyof typeof ShopTestStatus]

export const ShopStatusText: Record<ShopStatusType, string> = {
  [ShopStatus.DISABLED]: '禁用',
  [ShopStatus.ENABLED]: '启用',
  [ShopStatus.ABNORMAL]: '异常'
}

export const ShopTestStatusText: Record<ShopTestStatusType, string> = {
  [ShopTestStatus.NEVER]: '未测试',
  [ShopTestStatus.SUCCESS]: '成功',
  [ShopTestStatus.FAIL]: '失败'
}
