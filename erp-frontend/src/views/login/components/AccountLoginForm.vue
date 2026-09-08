<template>
  <a-form :model="modelRef">
    <a-form-item v-bind="validateInfos.username">
      <a-input
        v-model:value="modelRef.username"
        size="large"
        :placeholder="t('auth.username')"
        @press-enter="trySubmit"
      >
        <template #prefix>
          <user-outlined :style="{ color: 'rgba(0,0,0,.25)' }" />
        </template>
      </a-input>
    </a-form-item>

    <a-form-item v-bind="validateInfos.password">
      <a-input-password
        v-model:value="modelRef.password"
        size="large"
        :placeholder="t('auth.password')"
        autocomplete="on"
        @press-enter="trySubmit"
      >
        <template #prefix>
          <lock-outlined :style="{ color: 'rgba(0,0,0,.25)' }" />
        </template>
      </a-input-password>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import { authenticationManagers } from '@/api/auth'
import { Form } from 'ant-design-vue'

import type { AccountLoginParam } from '@/api/auth/types'
import type { LoginFormInstance } from './types'
import { passEncrypt } from '@/utils/password-utils'

import { authenticationType } from '@/config'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 登录表单参数
const useForm = Form.useForm

const modelRef = reactive<AccountLoginParam>({
  username: '',
  password: ''
})
const rulesRef = computed(() => ({
  username: [{ required: true, message: t('auth.usernameRequired') }],
  password: [{ required: true, message: t('auth.passwordRequired') }]
}))
const { validate, validateInfos } = useForm(modelRef, rulesRef)

// 定义事件
const emits = defineEmits<{
  (e: 'trySubmit'): void
}>()

// 尝试提交表单
function trySubmit() {
  emits('trySubmit')
}

defineExpose<LoginFormInstance>({
  validate,
  doLogin(captchaId) {
    const authenticationManager = authenticationManagers[authenticationType]
    return authenticationManager.login({
      username: modelRef.username,
      password: passEncrypt(modelRef.password), // 密码加密
      captchaId // 验证码id
    })
  }
})
</script>
