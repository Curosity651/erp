import { createI18n, type Locale } from 'vue-i18n'
import { useI18nStore } from '@/stores/i18n-store'
import dayjs from 'dayjs'
import { localMapping } from '@/locales/dayjs'
import type { App } from 'vue'
import { defaultLanguage } from '@/config'
import { isSupportedLocale, resolveInitialLocale, type SupportedLocale } from './locale-contract'

export const i18n = createI18n({
  legacy: false,
  locale: '',
  messages: {},
  fallbackLocale: defaultLanguage
})

const localesMap = Object.fromEntries(
  Object.entries(import.meta.glob('./lang/*.ts')).map(([path, loadLocale]) => [
    path.match(/([\w-]*)\.ts$/)?.[1],
    loadLocale
  ])
) as Record<Locale, () => Promise<{ default: Record<string, string> }>>

// 过滤掉 default-local-import 这个非真实语言项，避免出现在语言切换选项中
export const availableLocales = Object.keys(localesMap).filter(
  locale => locale !== 'default-local-import'
)

const loadedLanguages: string[] = []

function setI18nLanguage(lang: Locale) {
  dayjs.locale(localMapping[lang])
  i18n.global.locale.value = lang as any
  if (typeof document !== 'undefined') document.querySelector('html')?.setAttribute('lang', lang)
  return lang
}

export async function loadLanguageAsync(lang: string): Promise<SupportedLocale> {
  const locale = resolveInitialLocale(lang)
  // If the same language
  if (i18n.global.locale.value === locale) return setI18nLanguage(locale) as SupportedLocale

  // If the language was already loaded
  if (loadedLanguages.includes(locale)) return setI18nLanguage(locale) as SupportedLocale

  // If the language hasn't been loaded yet
  const loader = localesMap[locale]
  if (!loader || !isSupportedLocale(locale)) return setI18nLanguage(defaultLanguage) as SupportedLocale
  const messages = await loader()
  i18n.global.setLocaleMessage(locale, messages.default)
  loadedLanguages.push(locale)
  return setI18nLanguage(locale) as SupportedLocale
}

export const install = (app: App<Element>) => {
  app.use(i18n)
  const store = useI18nStore()
  const locale = resolveInitialLocale(store.language)
  store.setLanguage(locale)
  return loadLanguageAsync(locale)
}
