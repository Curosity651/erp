import type { PageParam } from '@/api/types'

/**
 * 销售目标
 */
export interface SalesTargetDTO {
  // ID
  id: number
  // 目标类型：年度或月度
  targetType: string
  // 目标年份
  targetYear: number
  // 目标月份，仅当 MONTHLY 时有值
  targetMonth: number
  // 目标销售额
  targetAmount: string
  // 货币单位
  currency: string
  // 创建人
  createdBy: number
  // 备注
  remark: string
}

export interface SalesTargetQO {
  // ID
  id?: number
  // 目标类型：年度或月度
  targetType?: string
  // 目标年份
  targetYear?: number
  // 目标月份，仅当 MONTHLY 时有值
  targetMonth?: number
  // 目标销售额
  targetAmount?: string
  // 货币单位
  currency?: string
  // 创建人
  createdBy?: number
  // 备注
  remark?: string
}

/**
 * 销售目标分页参数
 */
export type SalesTargetPageParam = SalesTargetQO & PageParam

/**
 * 销售目标分页视图对象
 */
export interface SalesTargetPageVO extends SalesTargetDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
  // 实际完成金额
  actualAmount?: number
  // 达成率(百分比)
  achievementRate?: number
}

/**
 * 月度目标输入DTO
 */
export interface MonthlyTargetInputDTO {
  // 月份 (1-12)
  month: number
  // 目标金额
  amount?: number
  // 备注
  remark?: string
}

/**
 * 年度目标输入DTO
 */
export interface AnnualTargetInputDTO {
  // 年度目标金额
  amount: number
  // 备注
  remark?: string
}

/**
 * 销售目标批量创建DTO
 */
export interface SalesTargetBatchCreateDTO {
  // 目标年份
  year: number
  // 货币单位
  currency: string
  // 年度目标
  annualTarget: AnnualTargetInputDTO
  // 月度目标列表
  monthlyTargets?: MonthlyTargetInputDTO[]
}

/**
 * 销售目标更新DTO
 */
export interface SalesTargetUpdateDTO {
  // 目标ID
  id: number
  // 目标金额
  targetAmount: number
  // 备注
  remark?: string
}

/**
 * 销售目标批量月度更新DTO
 */
export interface SalesTargetBatchMonthlyUpdateDTO {
  // 目标年份
  year: number
  // 更新目标列表
  targets: SalesTargetUpdateDTO[]
}

/**
 * 销售目标年度概览VO
 */
export interface SalesTargetYearlyOverviewVO {
  // 年度目标
  annualTarget?: SalesTargetPageVO
  // 月度目标列表
  monthlyTargets: SalesTargetPageVO[]
  // 月度目标总和
  monthlySum: number
  // 差额(年度目标 - 月度总和)
  difference: number
  // 已设置的月度目标数量
  monthlyCount: number
  // 年度实际完成金额
  annualActualAmount?: number
  // 年度达成率(百分比)
  annualAchievementRate?: number
}
