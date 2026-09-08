<template>
  <a-form>
    <a-form-item name="mobile" :rules="[{ required: true, message: t('auth.mobileRequired') }]">
      <a-input size="large" :placeholder="t('auth.mobile')">
        <template #prefix>
          <mobile-outlined :style="{ color: 'rgba(0,0,0,.25)' }" />
        </template>
      </a-input>
    </a-form-item>

    <a-form-item
      name="captcha"
      :rules="[{ required: true, message: t('auth.verificationCodeRequired') }]"
    >
      <div style="display: flex; align-items: center">
        <a-input
          size="large"
          :placeholder="t('auth.verificationCode')"
          style="flex: 1; transition: width 0.3s; margin-right: 8px"
        >
          <template #prefix>
            <mail-outlined :style="{ color: 'rgba(0,0,0,.25)' }" />
          </template>
        </a-input>
        <a-button style="display: block; height: 40px">
          {{ t('auth.getVerificationCode') }}
        </a-button>
      </div>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
// const loginStatus = ref('none')
// const loginErrorMessage = ref('手机验证码错误!')
</script>

<style scoped></style>
