<template>
  <div :class="getCls('container')">
    <div :class="getCls('top')">
      <div :class="getCls('header')">
        <!-- logoAndTitle -->
        <span :class="getCls('logo')">
          <img alt="logo" src="@/assets/logo.png" />
        </span>
        <!-- 标题 -->
        <span :class="getCls('title')"> {{ projectTitle }} </span>
      </div>
      <!-- 描述 -->
      <div :class="getCls('desc')">{{ t('auth.description') }}</div>
    </div>

    <div :class="getCls('main')" style="width: 368px">
      <a-tabs v-model:active-key="currentLoginType" class="login-tabs">
        <a-tab-pane key="account" :tab="t('auth.accountLogin')"></a-tab-pane>
        <!-- <a-tab-pane key="mobile" tab="手机号登录"></a-tab-pane> -->
      </a-tabs>

      <!-- 登录入口选择（三入口，错入口登录被拒） -->
      <a-radio-group
        v-model:value="currentEntryType"
        button-style="solid"
        size="large"
        class="entry-selector"
        style="display: flex; margin-bottom: 20px"
      >
        <a-radio-button value="ERP_USER" style="flex: 1; text-align: center">
          {{ t('auth.owner') }}
        </a-radio-button>
        <a-radio-button value="WMS_OPERATOR" style="flex: 1; text-align: center">
          {{ t('auth.provider') }}
        </a-radio-button>
        <a-radio-button value="OVERSEAS_PLATFORM" style="flex: 1; text-align: center">
          {{ t('auth.platform') }}
        </a-radio-button>
      </a-radio-group>

      <!-- 错误提示信息 -->
      <a-alert
        v-if="isLoginError"
        style="margin-bottom: 24px"
        :message="loginErrorMessage"
        type="error"
        show-icon
      />

      <!-- 账户密码登录 -->
      <account-login-form
        v-show="currentLoginType === 'account'"
        ref="accountLoginFormRef"
        @try-submit="handleLogin"
      />

      <!-- 手机号登录 -->
      <mobile-login-form v-show="currentLoginType === 'mobile'" ref="mobileLoginFormRef" />

      <div style="margin-bottom: 24px">
        <a-checkbox v-model:checked="rememberMe" no-style name="autoLogin">
          {{ t('auth.autoLogin') }}
        </a-checkbox>
        <!-- <a style="float: right">忘记密码</a> -->
      </div>

      <a-button
        size="large"
        type="primary"
        style="width: 100%"
        :loading="loginLoading"
        @click="handleLogin"
      >
        {{ rawI18nText('user.login.submit', '登录') }}
      </a-button>

      <!-- 扩展部分 -->
      <!-- <div :class="getCls('other')">
        <a-space :size="8">
          <span>其他登录方式</span>
          <alipay-outlined class="icon" />
          <taobao-outlined class="icon" />
          <weibo-outlined class="icon" />
        </a-space>
        <a style="float: right"> 注册账户 </a>
      </div> -->
    </div>

    <!-- 登陆验证码 -->
    <login-captcha v-if="enableLoginCaptcha" ref="loginCaptchaRef" @success="handleSubmit" />
  </div>
</template>

<script setup lang="ts">
import AccountLoginForm from '@/views/login/components/AccountLoginForm.vue'
import MobileLoginForm from '@/views/login/components/MobileLoginForm.vue'
import type { LoginFormInstance, LoginType } from '@/views/login/components/types'
import type { LoginResult } from '@/api/auth/types'
import { useUserStore } from '@/stores/user-store'
import { projectTitle, enableLoginCaptcha } from '@/config'
import { SliderCaptcha as LoginCaptcha } from '@/components/Captcha'
import { useAdminI18n } from '@/hooks/i18n'
import { getCurrentTenantIdentity } from '@/api/tenant'
import { isSuccess } from '@/api'

const { rawI18nText } = useAdminI18n()
const { t } = useI18n()

const userStore = useUserStore()

// 当前登录入口（三入口）：货主 / WMS服务商 / 平台超管
const currentEntryType = ref<'ERP_USER' | 'WMS_OPERATOR' | 'OVERSEAS_PLATFORM'>('ERP_USER')

const prefixCls = 'ant'
const baseClassName = 'pro-login-content'

function getCls(className: string) {
  return `${prefixCls}-${baseClassName}-${className}`
}

// 登录的加载状态
const loginLoading = ref(false)
// 登陆错误
const isLoginError = ref(false)
// 登录错误信息
const loginErrorMessage = ref('')
// 自动登录（记住我）
const rememberMe = ref(false)

// 登陆验证码组件
const loginCaptchaRef = ref()

// 当前登录类型，以及对应的登录组件
const currentLoginType = ref<LoginType>('account')
let loginFormRef = ref<LoginFormInstance>()
const accountLoginFormRef = ref<LoginFormInstance>()
const mobileLoginFormRef = ref<LoginFormInstance>()

watchEffect(() => {
  switch (currentLoginType.value) {
    case 'account':
      loginFormRef = accountLoginFormRef
      break
    case 'mobile':
      loginFormRef = mobileLoginFormRef
      break
  }
})

/** 存储登录信息 */
function store(res: LoginResult) {
  // 存储 token
  userStore.accessToken = res.access_token

  // 存储用户信息
  const info = res.info
  const roleCodes = res.attributes?.roleCodes || []
  const permissions = res.attributes?.permissions || []
  userStore.userInfo = {
    ...info,
    roleCodes,
    permissions
  }

  // TODO 自动登录处理
  // const ttl = res.expires_in * 1000
  // const refreshToken = res.refresh_token
}

function handleLogin() {
  const loginFormInstance = loginFormRef.value!
  loginFormInstance.validate().then(() => {
    enableLoginCaptcha ? loginCaptchaRef.value?.show() : handleSubmit()
  })
}

const router = useRouter()

async function handleSubmit(captchaId?: string) {
  const loginFormInstance = loginFormRef.value!
  loginLoading.value = true
  try {
    const res = await loginFormInstance.doLogin(captchaId)
    store(res)

    // B2：登录后校验「所选入口」与「账号真实身份」是否匹配
    const identityRes = await getCurrentTenantIdentity(currentEntryType.value)
    if (!isSuccess(identityRes)) {
      // 入口不匹配 / 未绑定租户：清理登录态，停留在登录页
      userStore.clean()
      isLoginError.value = true
      loginErrorMessage.value = identityRes.message || t('auth.wrongEntry')
      return
    }
    userStore.setTenantIdentity(identityRes.data)

    isLoginError.value = false
    const nextPath = (router.currentRoute.value.query.redirect as string) || '/'
    router.push(nextPath)
  } catch (err: any) {
    isLoginError.value = true
    loginErrorMessage.value =
      ((err.response || {}).data || {}).message || t('auth.requestFailed')
  } finally {
    loginLoading.value = false
  }
}
</script>

<style lang="less">
@import 'loginContent.less';

.login-tabs .ant-tabs-tab {
  padding: 12px 16px !important;
}
</style>
