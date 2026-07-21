import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

/**
 * 用户信息VO
 */
export interface UserVO {
  // 用户ID
  id: string
  // 用户名
  username: string
  // 真实姓名
  realName: string
  // 邮箱
  email?: string
  // 手机号
  phone?: string
  // 部门ID
  deptId?: number
  // 部门名称
  deptName?: string
  // 岗位列表
  positions?: string[]
}

/**
 * 用户岗位枚举
 */
export enum UserPosition {
  DEVELOPER = 'developer', // 开发人员
  OPERATOR = 'operator', // 运营人员
  QC = 'qc', // 质检人员
  PURCHASER = 'purchaser' // 采购人员
}

/**
 * 岗位配置
 */
export const POSITION_CONFIG = {
  [UserPosition.DEVELOPER]: {
    name: '开发人员',
    code: 'developer'
  },
  [UserPosition.OPERATOR]: {
    name: '运营人员',
    code: 'operator'
  },
  [UserPosition.QC]: {
    name: '质检人员',
    code: 'qc'
  },
  [UserPosition.PURCHASER]: {
    name: '采购人员',
    code: 'purchaser'
  }
}

/**
 * 根据岗位获取用户列表
 * @param position 岗位代码
 */
export function getUsersByPosition(position: string) {
  return httpClient.get<ApiResult<UserVO[]>>('/system/user/list-by-position', {
    params: { position }
  })
}

/**
 * 获取所有岗位选项
 */
export function getPositionOptions() {
  return Object.entries(POSITION_CONFIG).map(([key, value]) => ({
    label: value.name,
    value: key,
    code: value.code
  }))
}

/**
 * 根据岗位代码获取岗位名称
 * @param position 岗位代码
 */
export function getPositionName(position: string): string {
  return POSITION_CONFIG[position as UserPosition]?.name || position
}

/**
 * 批量获取多个岗位的用户列表
 * @param positions 岗位代码列表
 */
export async function getUsersByMultiplePositions(positions: string[]) {
  const promises = positions.map(position => getUsersByPosition(position))
  const results = await Promise.all(promises)

  const userMap: Record<string, UserVO[]> = {}
  positions.forEach((position, index) => {
    userMap[position] = results[index].data || []
  })

  return userMap
}
