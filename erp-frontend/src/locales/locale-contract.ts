export const SUPPORTED_LOCALES = ['zh-CN', 'en-US', 'uk-UA', 'ru-RU'] as const

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'

export const isSupportedLocale = (value: string): value is SupportedLocale =>
  SUPPORTED_LOCALES.includes(value as SupportedLocale)

export const resolveInitialLocale = (value?: string | null): SupportedLocale =>
  value && isSupportedLocale(value) ? value : DEFAULT_LOCALE
