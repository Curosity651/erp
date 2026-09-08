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
          {{ t('auth.logout') }}
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
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

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
    identityText = userInfo?.tenantName
      ? `${t('auth.platform')} · ${userInfo.tenantName}`
      : t('auth.platform')
    identityColor = 'red'
  } else if (identityType === 'WMS_OPERATOR') {
    identityText = userInfo?.tenantName
      ? `${t('auth.provider')} · ${userInfo.tenantName}`
      : t('auth.provider')
    identityColor = 'gold'
  } else if (identityType === 'ERP_USER') {
    identityText = userInfo?.tenantName
      ? `${t('auth.owner')} · ${userInfo.tenantName}`
      : t('auth.owner')
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
    title: t('auth.logoutTitle'),
    content: t('auth.logoutConfirm'),
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
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
