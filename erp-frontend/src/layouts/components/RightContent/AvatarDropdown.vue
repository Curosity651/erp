<template>
  <span v-if="loading" :class="[$style.action, $style.account]">
    <a-spin size="small" style="margin-left: 8px; margin-right: 8px" />
  </span>
  <header-dropdown v-else overlay-class-name="avatar-dropdown">
    <span :class="[$style.action, $style.account]">
      <a-avatar size="small" :class="$style.avatar" :src="currentUser.avatar" alt="avatar" />
      <span :class="[$style.name, 'anticon']">{{ currentUser.nickname }}</span>
      <a-tag
        v-if="currentUser.identityText"
        :color="currentUser.identityColor"
        style="margin-left: 8px"
      >
        {{ currentUser.identityText }}
      </a-tag>
    </span>

    <template #overlay>
      <a-menu :class="$style.menu" @click="onMenuClick">
        <!-- <template v-if="props.menu">
          <a-menu-item key="center">
            <UserOutlined />
            个人中心
          </a-menu-item>
          <a-menu-item key="settings">
            <SettingOutlined />
            个人设置
          </a-menu-item>
          <a-menu-divider style="margin: 4px 0" />
        </template> -->

        <a-menu-item key="logout">
          <LogoutOutlined />
          退出登录
        </a-menu-item>
      </a-menu>
    </template>
  </header-dropdown>
</template>

<script setup lang="ts">
import HeaderDropdown from '@/layouts/components/HeaderDropdown'
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface'
import { Modal } from 'ant-design-vue'
import { authenticationManagers, loginStateManagers } from '@/api/auth'
import { useUserStore } from '@/stores/user-store'
import { authenticationType, authenticationMethod, loginPath } from '@/config'
import { fileAbsoluteUrl } from '@/utils/file-utils'

// 目前不支持接口导入: https://github.com/vuejs/core/issues/4294
type GlobalHeaderRightProps = {
  menu?: boolean
}

const props = withDefaults(defineProps<GlobalHeaderRightProps>(), {
  menu: true
})
const loading = ref(false)

const userStore = useUserStore()

const currentUser = computed(() => {
  const userInfo = userStore.userInfo
  const identityType = userInfo?.identityType
  let identityText = ''
  let identityColor = 'blue'
  if (identityType === 'OVERSEAS_PLATFORM') {
    identityText = userInfo?.tenantName ? `海外仓平台 · ${userInfo.tenantName}` : '海外仓平台'
    identityColor = 'red'
  } else if (identityType === 'WMS_OPERATOR') {
    identityText = userInfo?.tenantName ? `WMS服务商 · ${userInfo.tenantName}` : 'WMS服务商'
    identityColor = 'gold'
  } else if (identityType === 'ERP_USER') {
    identityText = userInfo?.tenantName ? `货主 · ${userInfo.tenantName}` : '货主'
    identityColor = 'blue'
  }
  return {
    nickname: userInfo?.nickname,
    avatar: userInfo?.avatar ? fileAbsoluteUrl(userInfo?.avatar) : '',
    identityText,
    identityColor
  }
})

const router = useRouter()

const loginOut = () => {
  Modal.confirm({
    title: '提示',
    content: '确定要退出登录吗 ?',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      // 如果判断已经登出了，直接跳转登录页
      const loginStateManager = loginStateManagers[authenticationMethod]
      if (loginStateManager?.isLoggedOut()) {
        setTimeout(() => {
          router.push(loginPath)
        }, 200)
        return
      }
      // 否则的话，去服务端进行一次登出处理
      const authenticationManager = authenticationManagers[authenticationType]
      authenticationManager.logout().then(() => {
        userStore.clean()
        setTimeout(() => {
          router.push(loginPath)
        }, 200)
      })
    }
  })
}

const onMenuClick = (event: MenuInfo) => {
  const { key } = event
  if (key === 'logout') {
    loginOut()
  }
}
</script>

<script lang="ts">
export default {
  name: 'AvatarDropdown'
}
</script>

<style lang="less" module>
@import './index.less';
</style>
