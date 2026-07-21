import { ref, computed } from 'vue'
import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

export interface UserOption {
  value: number
  name: string
  username?: string
  email?: string
}

// 全局用户数据状态
const allUsers = ref<UserOption[]>([])
const loading = ref(false)
const isInitialized = ref(false)

// 一次性加载所有用户数据
const loadAllUsers = async (): Promise<UserOption[]> => {
  if (isInitialized.value) {
    return allUsers.value
  }

  loading.value = true
  try {
    const response = await httpClient.get<ApiResult<UserOption[]>>('/system/user/dropdown', {})

    if (response && response.code === 200) {
      allUsers.value = response.data || []
      isInitialized.value = true
      console.log('用户数据加载成功，共', allUsers.value.length, '个用户')
    } else {
      console.error('获取用户列表失败:', response?.message || '未知错误')
      allUsers.value = []
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    allUsers.value = []
  } finally {
    loading.value = false
  }

  return allUsers.value
}

// 本地搜索用户
const searchUsers = (keyword: string): UserOption[] => {
  if (!keyword.trim()) {
    return allUsers.value
  }

  const lowerKeyword = keyword.toLowerCase().trim()
  return allUsers.value.filter(user => user.name.toLowerCase().includes(lowerKeyword))
}

// 根据用户ID查找用户信息
const findUserById = (userId: number): UserOption | undefined => {
  return allUsers.value.find(user => user.value === userId)
}

// 获取用户名（找不到返回占位符）
const getUserName = (userId: number): string => {
  const user = findUserById(userId)
  return user?.name ?? `用户${userId}`
}

// 批量查找用户
const findUsersByIds = (userIds: number[]): UserOption[] => {
  return userIds.map(id => findUserById(id)).filter((u): u is UserOption => !!u)
}

// 确保用户在选项列表中
const ensureUserInOptions = async (userId: number): Promise<UserOption | undefined> => {
  // 先在当前列表中查找
  let user = findUserById(userId)
  if (user) {
    return user
  }

  // 如果没找到且还没初始化，先加载所有数据
  if (!isInitialized.value) {
    await loadAllUsers()
    user = findUserById(userId)
  }

  return user
}

export function useUserData() {
  return {
    // 状态
    allUsers: computed(() => allUsers.value),
    loading: computed(() => loading.value),
    isInitialized: computed(() => isInitialized.value),

    // 方法
    loadAllUsers,
    searchUsers,
    findUserById,
    findUsersByIds,
    getUserName,
    ensureUserInOptions,

    // 工具方法
    clearCache: () => {
      allUsers.value = []
      isInitialized.value = false
    }
  }
}
