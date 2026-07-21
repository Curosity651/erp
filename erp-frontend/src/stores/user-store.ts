import { defineStore } from 'pinia'
import { useLocalStorage } from '@vueuse/core'
import { getStorageKey } from '@/utils/storage-utils'
import type { LoginUserInfo } from '@/api/auth/types'
import { getLoginUserMenus } from '@/api/system/menu'
import type { SysMenuRouterVO } from '@/api/system/menu/types'
import type { TenantIdentity } from '@/api/tenant/types'
import { filterMenusByIdentity } from '@/utils/menu-filter'
import { useMultiTabStore } from '@/stores/multitab-store'

export interface UserInfo extends LoginUserInfo {
  roleCodes: string[]
  permissions: string[]
  // 身份（登录后通过 /tenant/current 补充）
  identityType?: string
  tenantId?: number
  tenantType?: string
  tenantCode?: string
  tenantName?: string
  admin?: boolean
}

const accessTokenKey = getStorageKey('access-token')
const userInfoKey = getStorageKey('user-info')

// 使用 setup 语法重构，解决 Vue 3.5 + VueUse 存储的兼容性问题。
// 登录态用 localStorage（全浏览器共享）：本系统服务端是 FORM_LOGIN + JSESSIONID cookie 鉴权，
// cookie 按源共享、同一浏览器同一时刻只有一个服务端会话。前端身份也随之全浏览器统一，避免出现
// 「前端按标签页显示某身份，但实际骑着另一个共享会话」的错位。新标签页因此能读到身份并正确裁剪菜单。
// 注：同一浏览器同时只能登录一个身份；需多身份并行请用不同浏览器/无痕窗口。
export const useUserStore = defineStore('userStore', () => {
  // State
  const accessToken = useLocalStorage<string | undefined>(accessTokenKey, undefined, {
    writeDefaults: false
  })
  const userInfo = useLocalStorage<UserInfo | undefined>(userInfoKey, undefined, {
    writeDefaults: false,
    serializer: {
      read: (v: string) => {
        if (!v || v === 'undefined') return undefined
        try {
          return JSON.parse(v)
        } catch {
          return undefined
        }
      },
      write: (v: UserInfo | undefined) => {
        return v === undefined ? '' : JSON.stringify(v)
      }
    }
  })
  const userMenus = ref<SysMenuRouterVO[] | undefined>(undefined)

  // Actions
  async function fetchUserMenus() {
    const { data } = await getLoginUserMenus()
    // 按身份裁剪菜单（超管全部；货主隐藏 WMS；服务商仅 WMS+系统+个人页）
    const filtered = filterMenusByIdentity(data, userInfo.value?.identityType)
    userMenus.value = filtered
    return filtered
  }

  /** 登录后写入身份 */
  function setTenantIdentity(identity: TenantIdentity) {
    if (!userInfo.value) return
    userInfo.value = {
      ...userInfo.value,
      identityType: identity.identityType,
      tenantId: identity.tenantId,
      tenantType: identity.tenantType,
      tenantCode: identity.tenantCode,
      tenantName: identity.tenantName,
      admin: identity.admin
    }
  }

  function clean() {
    accessToken.value = undefined
    userInfo.value = undefined
    userMenus.value = undefined
    // 清空多标签页与组件缓存：否则换身份登录后，上一身份打开的标签仍残留，
    // 而动态路由已按新身份重建并裁剪 → 点击残留标签报「当前网页不存在」。
    useMultiTabStore().$reset()
  }

  return {
    accessToken,
    userInfo,
    userMenus,
    fetchUserMenus,
    setTenantIdentity,
    clean
  }
})
