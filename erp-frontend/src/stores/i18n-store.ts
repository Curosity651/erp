import { defineStore } from 'pinia'
import { useLocalStorage } from '@vueuse/core'
import { getStorageKey } from '@/utils/storage-utils'

import { defaultLanguage } from '@/config'
import { resolveInitialLocale, type SupportedLocale } from '@/locales/locale-contract'

const i18nKey = getStorageKey('language')

export const useI18nStore = defineStore('i18nStore', {
  state: () => ({
    language: useLocalStorage<SupportedLocale>(i18nKey, defaultLanguage, {
      writeDefaults: true
    })
  }),

  actions: {
    setLanguage(language: string) {
      this.language = resolveInitialLocale(language)
    },
    reset() {
      this.language = defaultLanguage
    }
  }
})
