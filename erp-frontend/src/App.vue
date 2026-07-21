<template>
  <a-style-provider :hash-priority="'high'">
    <a-config-provider :locale="antdLocal" :theme="theme">
      <router-view />
    </a-config-provider>
  </a-style-provider>
</template>

<script setup lang="ts">
import { theme as antdTheme } from 'ant-design-vue'
import { useI18nStore } from '@/stores/i18n-store'
import { useI18n } from 'vue-i18n'
import { enableI18n } from '@/config'
import { useAntdCssVar } from '@/hooks/use-antd-css-var'
import type { Locale } from 'ant-design-vue/es/locale'
import type { Ref } from 'vue'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'
import dayjs from 'dayjs'

// 注入 antd token 为全局 CSS Variables
useAntdCssVar()

// 主题配置
const theme: ThemeConfig = {
  token: {},
  algorithm: antdTheme.defaultAlgorithm
}

let antdLocal: Ref<Locale>

if (enableI18n) {
  const i18n = useI18n()
  const i18nStore = useI18nStore()
  antdLocal = computed<Locale>(() => {
    return i18n.getLocaleMessage(i18nStore.language)?.antdLocale as Locale
  })
} else {
  // 未开启国际化，默认使用中文
  const modules = import.meta.glob('@/locales/lang/default-local-import.ts', { eager: true })
  for (const path in modules) {
    // @ts-ignore
    antdLocal = modules[path].default
  }
  dayjs.locale('zh-cn')
}
</script>
