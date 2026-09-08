<template>
  <a-dropdown>
    <TranslationOutlined style="font-size: 18px" />

    <template #overlay>
      <a-menu :selected-keys="[i18nStore.language]">
        <a-menu-item
          v-for="language of supportLanguage"
          :key="language.lang"
          @click="switchLanguage(language.lang)"
        >
          <span role="img" :aria-label="language.title">{{ language.symbol }}</span>
          {{ language.title }}
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>
<script setup lang="ts">
import { TranslationOutlined } from '@ant-design/icons-vue'
import { useI18nStore } from '@/stores/i18n-store'
import { loadLanguageAsync } from '@/locales'
import { useUserStore } from '@/stores/user-store'
import { generatorDynamicRouter } from '@/router/dynamic-routes'
import router, { resetRouter } from '@/router'
import { emitter } from '@/hooks/mitt'
import { supportLanguage } from '@/config'
import { message } from 'ant-design-vue'
import { i18n } from '@/locales'
import { isSupportedLocale } from '@/locales/locale-contract'

const i18nStore = useI18nStore()
const userStore = useUserStore()

const switchLanguage = async (locale: string) => {
  if (!isSupportedLocale(locale) || locale === i18nStore.language) return
  const previousLocale = i18nStore.language
  try {
    await loadLanguageAsync(locale)
    i18nStore.setLanguage(locale)
    const userMenus = await userStore.fetchUserMenus()
    const dynamicRouter = generatorDynamicRouter(userMenus)
    resetRouter()
    router.addRoute(dynamicRouter)
    emitter.emit('switch-language', locale)
  } catch {
    await loadLanguageAsync(previousLocale)
    message.error(i18n.global.t('locale.switchFailed'))
  }
}
</script>

<script lang="ts">
export default {
  name: 'SelectLanguage'
}
</script>
